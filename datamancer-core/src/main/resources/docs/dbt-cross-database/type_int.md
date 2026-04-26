---
title: "type_int"
sidebar_label: "type_int"
id: "type_int"
description: "This macro yields the database-specific data type for an INT."
---

This macro yields the database-specific data type for an `INT`.

**Usage**:

```sql
{{ dbt.type_int() }}
```

**Sample Output (PostgreSQL)**:

```sql
INT
```
