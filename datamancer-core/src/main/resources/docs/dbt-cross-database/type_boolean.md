---
title: "type_boolean"
sidebar_label: "type_boolean"
id: "type_boolean"
description: "This macro yields the database-specific data type for a BOOLEAN."
---

This macro yields the database-specific data type for a `BOOLEAN`.

**Usage**:

```sql
{{ dbt.type_boolean() }}
```

**Sample Output (PostgreSQL)**:

```sql
BOOLEAN
```
