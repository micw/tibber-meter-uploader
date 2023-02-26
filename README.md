# Tibber Meter Uploader

Dieses Tool verwendet die Tibber-API, welcher unter https://app.tibber.com/v4/gql verfügbar ist, um tägliche Zählerstände automatisiert hochzuladen.

Es kann als Alternative zur manuellen monatlichen Eingabe von Zählerständen verwendet werden und ist nicht als Ersatz für den "Tibber Pulse" gedacht, welcher stündliche Werte übermittelt.

## Bauen

Das Projekt kann mit Java und Maven gebaut werden (`mvn package`). Das Weiteren liegt ein Dockerfile bei, welches ein Packaging via `docker build . -t tibber-meter-uploader` ermöglicht.

Automatisierte Docker-Builds sind unter ghcr.io/micw/tibber-meter-uploader verfügbar.

## Ausführen (docker)

```
docker run -it --rm \
  -e "READINGS_SOURCE_CLASS=ScriptedRestApiMeterReadingSource" \
  -e "READINGS_SCRIPT_COMMAND=echo test; exit 1" \
  -e TIBBER_LOGIN=me@example.com \
  -e TIBBER_PASSWORD=mysecretpassword \
  ghcr.io/micw/tibber-meter-uploader:master
```

## Ausführen (nativ)

Pre-built jars can be downloaded from https://mega.nz/folder/pi4yjaoI#OXNDwnkfyH6xOEJEdtN3pg . To run it, you need a Java Runtime Environment (JRE) with version 11 or higher installed. COnfig can be passed as environment variables or by creating `appliucation.yaml` in the working directory (e.g. next to the downloaded jar file).

Example:

```
echo "TIBBER_LOGIN: me@example.com" > application.yaml
echo "TIBBER_PASSWORD: mysecretpassword" >> application.yaml
java -Xmx25M -jar tibber-meter-uploader.master.jar 
```

Memory assignment of the process can be tuned by the `-Xmx` option - adjust it to your needs so that the process does not get an out of memory error.

## Konfiguration

Die Konfiguration erfolgt über eine Konfigurationsdatei (`application.yaml`, siehe Beispiel im Wurzelverzeichnis) oder über Umgebungsvariablen.

* `TIBBER_LOGIN` (benötigt): E-Mail-Adresse eines Tibber-Accounts
* `TIBBER_PASSWORD` (benötigt): Passwort eines Tibber-Accounts
* `READINGS_SOURCE_CLASS` (benötigt): Implementierungsklasse der Quelle für Zählerstände (siehe unten)
* `SCHEDULING_ENABLED` (default: `true`): Wenn der Parameter auf `false` gesetzt wird, terminiert der Prozess nach einem einmaligen Durchlauf
* `SCHEDULING_CRON` (default: `0 0 * * * *` = jede volle Stunde): Ermöglicht, den Ausführungszeitpunkt der regelmäßigen Durchläufe zu verändern
* `DRY_RUN` (default: `false`): Wenn der Parameter auf `true` gesetzt wird, werden die an an Tibber zu übermittelnden Zählerstände nur angezeigt, aber nicht übertragen. Nützlich, um Quellen und die Konfiguration zu testen.

### Meter Register ID

In einigen Fällen ist bei Tibber nicht der Standard-OBIS-Code `1-1:1.8.0` für den Gesamt-Strombezug hinterlegt sondern `1-1:1.8.0`. In dem Fall erscheint beim Start eine Fehlermeldung ähnlich dieser:

	Meter 149d2526-6c26-4435-9b2b-0dbfd3251bcd has no register with id '1-0:1.8.0'. Available registers are: 1-1:1.8.0

Über den Konfigurtationsparameter `TIBBER_METER_REGISTER_ID = 1-1:1.8.0` kann die Anwendung so konfiguriert werden, dass Zählerstände für diesen OBIS-Code an Tibber übergeben werden.

Eine Liste gängiger OBIS-Codes und deren Bedeutung kann unter https://de.wikipedia.org/wiki/OBIS-Kennzahlen gefunden werden.

## Programmablauf

* Beim Start sowie einmal pro volle Stunde versucht sich der Client an der Tibber-API anzumelden und das Benutzerprofil incl. der zuletzt gemeldeten Zählerstände abzurufen
* Es wird derzeit nur ein Zuhause mit einem Zähler unterstützt
* Sind für ein oder mehrere Tage in der Vergangenheit (maximal 30 Tage zurück) noch keine Zählerstände vorhanden, wird die konfigurierte Quelle nach Zählerständen in diesem Zeitraum befragt
* fehlende Zählerstände werden nachgetragen

## Quellen

Um flexibel zu sein, unterstützt das Tool konfigurierbare Quellen für die Zählerstände.

### ScriptedRestApiMeterReadingSource

Diese Quelle führt ein Shell-Script aus, um Zählerstände zu beziehen. Als Ergebnis wird eine Liste mit je einem Datum + Zählerstand in kWh pro Zeile erwartet (getrennt mit Leerzeichen, Semikolon oder Komma).

Beispiel:

```
2023-01-19 10003
2023-01-20 10114
2023-01-21 10234
2023-01-22 10521
```

Die folgenden Konfigurationsparameter sind für die Quelle verfügbar:

* `READINGS_SOURCE_CLASS` (benötigt): `ScriptedRestApiMeterReadingSource` für diese Quelle
* `READINGS_SCRIPT_COMMAND` (benötigt): Auszuführender Befehl oder Shell-Script. Der Befehl wird an eine Shell mittels `sh -c ${READINGS_SCRIPT_COMMAND}` übergeben
* `READINGS_METER`(optional): Wenn angegeben, prüft die Quelle, dass die von der Tibber-Api gelieferte Zählernummer dieser Zählernummer entspricht.

Innerhalb des Shell-Scriptes stehen die folgenden Umgebnugsvariablen zur Verfügung:

* `FIRST_DAY` - Der erste Tag, für den der Zählerstand benötigt wird. Format: `2023-01-19`
* `FIRST_DAY` - Der letzte Tag, für den der Zählerstand benötigt wird. Format: `2023-01-22`
* `METER` - Die abgefragte Zähelernummer. Format: `1EBZ0123456789`
* `FIRST_DAY_START_ISO_TZ` - Die Startzeit des ersten Tages, Format: `2023-01-19T00:00:00+01:00[Europe/Berlin]`
* `LAST_DAY_END_ISO_TZ` - Die Startzeit des Folgetages des letzten Tages. Format: `2023-01-23T00:00:00+01:00[Europe/Berlin]`


### CommandLineMeterReadingSource

Diese Quelle ließt Zählerstände von der Kommandozeile. Es können mehrere Zählerstände im Format datum=zählerstand übergeben werden. Statt des Datums kann auch das Schlüsselwort `today` verwendet werden, um den aktuellen Tag zu übergeben.

Beispiel:

```
java -jar tibber-uploader.jar 2023-01-19=10003 2023-01-20=10114 2023-01-21=10234 today=10521
```

Die folgenden Konfigurationsparameter sind für die Quelle verfügbar:

* `READINGS_SOURCE_CLASS` (benötigt): `CommandLineMeterReadingSource` für diese Quelle
* `READINGS_METER`(optional): Wenn angegeben, prüft die Quelle, dass die von der Tibber-Api gelieferte Zählernummer dieser Zählernummer entspricht.

Es ist sinnvoll, diese Quelle zusammen mit dem Konfigurationsparameter `SCHEDULING_ENABLED=false` zu verwenden, um das Programm nach dem Upload der Werte zu beenden.

### DummyMeterReadingSource

Diese Quelle stellt ein einzelnes statisches Reading zum Testen zur Verfügung.

Die folgenden Konfigurationsparameter sind für die Quelle verfügbar:

* `READINGS_SOURCE_CLASS` (benötigt): `DummyMeterReadingSource` für diese Quelle
* `READINGS_METER`(optional): Wenn angegeben, prüft die Quelle, dass die von der Tibber-Api gelieferte Zählernummer dieser Zählernummer entspricht.
* `DUMMY_READING_DATE`(benötigt): Datum des Dummy-Readings, z.b. 2023-01-19
* `DUMMY_READING_VALUE`(benötigt): Wert des Dummy-Readings, z.b. 10003
