---
title: "type_bigint"
sidebar_label: "type_bigint"
id: "type_bigint"
description: "This macro yields the database-specific data type for a BIGINT."
---

This macro yields the database-specific data type for a `BIGINT`.

**Usage**:

```sql
{{ dbt.type_bigint() }}
```

**Sample Output (PostgreSQL)**:

```sql
bigint
```
