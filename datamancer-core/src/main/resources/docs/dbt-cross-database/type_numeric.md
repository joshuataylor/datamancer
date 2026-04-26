---
title: "type_numeric"
sidebar_label: "type_numeric"
id: "type_numeric"
description: "This macro yields the database-specific data type for a NUMERIC."
---

This macro yields the database-specific data type for a `NUMERIC`.

**Usage**:

```sql
{{ dbt.type_numeric() }}
```

**Sample Output (PostgreSQL)**:

```sql
numeric(28,6)
```
