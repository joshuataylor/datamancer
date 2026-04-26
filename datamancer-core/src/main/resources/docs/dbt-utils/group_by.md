---
title: "group_by"
sidebar_label: "group_by"
id: "group_by"
description: "This macro builds a group by statement for fields 1...N"
category: "SQL generators"
---

This macro builds a group by statement for fields 1...N

**Usage:**

```sql
{{ dbt_utils.group_by(n=3) }}
```

Would compile to:

```sql
group by 1,2,3
```
