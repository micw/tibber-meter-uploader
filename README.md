# Tibber Meter Uploader

Dieses Tool verwendet die Tibber-API, welcher unter https://app.tibber.com/v4/gql verfügbar ist, um tägliche Zählerstände automatisiert hochzuladen.

Es kann als Alternative zur manuellen monatlichen Eingabe von Zählerständen verwendet werden und ist nicht als Ersatz für den "Tibber Pulse" gedacht, welcher stündliche Werte übermittelt.

## Bauen

Das Projekt kann mit Java und Maven gebaut werden (`mvn package`). Das Weiteren liegt ein Dockerfile bei, welches einen Build via `docker build . -t tibber-meter-uploader` ermöglicht.

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

## Konfiguration

Die Konfiguration erfolgt über eine Konfigurationsdatei (`application.yaml`, siehe Beispiel im Wurzelverzeichnis) oder über Umgebungsvariablen.

* `TIBBER_LOGIN` (benötigt): E-Mail-Adresse eines Tibber-Accounts
* `TIBBER_PASSWORD` (benötigt): Passwort eines Tibber-Accounts
* `READINGS_SOURCE_CLASS` (benötigt): Implementierungsklasse der Quelle für Zählerstände (siehe unten)

## Programmablauf

* Beim Start sowie einmal pro volle Stunde versucht sich der Client an der Tibber-API anzumelden und das Benutzerprofil incl. der zuletzt gemeldeten Zählerstände abzurufen
* Es wird derzeit nur ein Zuhause mit einem Zähler unterstützt
* Sind für ein oder mehrere Tage in der Vergangenheit (maximal 30 Tage zurück) noch keine Zählerstände vorhanden, wird die konfigurierte Quelle nach Zählerständen in diesem Zeitraum befragt
* fehlende Zählerstände werden nachgetragen

## Quellen

Um flexibel zu sein, unterstützt das Tool konfigurierbare Quellen für die Zählerstände.

### ScriptedRestApiMeterReadingSource

Diese Quelle führt ein Shell-Script aus, um Zählerwerte zu beziehen. Als ergebnis wird eine Liste mit je einem Datum + Zählerstand in kWh pro Zeile erwartet (getrennt mit Leerzeichen, Semikolon oder Komma).

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
