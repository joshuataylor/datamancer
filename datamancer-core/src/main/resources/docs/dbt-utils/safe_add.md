---
title: "safe_add"
sidebar_label: "safe_add"
id: "safe_add"
description: "This macro implements a cross-database way to sum nullable fields using the fields specified."
category: "SQL generators"
---

This macro implements a cross-database way to sum nullable fields using the fields specified.

**Usage:**

```sql
{{ dbt_utils.safe_add(['field_a', 'field_b', ...]) }}
```
