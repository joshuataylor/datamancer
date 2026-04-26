---
title: "type_float"
sidebar_label: "type_float"
id: "type_float"
description: "This macro yields the database-specific data type for a FLOAT."
---

This macro yields the database-specific data type for a `FLOAT`.

**Usage**:

```sql
{{ dbt.type_float() }}
```

**Sample Output (PostgreSQL)**:

```sql
FLOAT
```
