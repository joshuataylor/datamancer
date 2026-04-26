---
title: "type_string"
sidebar_label: "type_string"
id: "type_string"
description: "This macro yields the database-specific data type for TEXT."
---

This macro yields the database-specific data type for `TEXT`.

**Usage**:

```sql
{{ dbt.type_string() }}
```

**Sample Output (PostgreSQL)**:

```sql
TEXT
```
