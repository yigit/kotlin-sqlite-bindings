Module to expose common SQLite related classes from the sqlitebindings artifact.

This allows ksqlite to have an internal dependency on sqlitebindings while
still exposing some parts as APIs (like ResultCode)