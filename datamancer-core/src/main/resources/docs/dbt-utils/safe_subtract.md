---
title: "safe_subtract"
sidebar_label: "safe_subtract"
id: "safe_subtract"
description: "This macro implements a cross-database way to take the difference of nullable fields using the fields specified."
category: "SQL generators"
---

This macro implements a cross-database way to take the difference of nullable fields using the fields specified.

**Usage:**

```sql
{{ dbt_utils.safe_subtract(['field_a', 'field_b', ...]) }}
```
