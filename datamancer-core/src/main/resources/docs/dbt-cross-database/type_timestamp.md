---
title: "type_timestamp"
sidebar_label: "type_timestamp"
id: "type_timestamp"
description: "This macro yields the database-specific data type for a TIMESTAMP."
---

This macro yields the database-specific data type for a `TIMESTAMP` (which may or may not match the behaviour of `TIMESTAMP WITHOUT TIMEZONE` from ANSI SQL-92).

**Usage**:

```sql
{{ dbt.type_timestamp() }}
```

**Sample Output (PostgreSQL)**:

```sql
TIMESTAMP
```
