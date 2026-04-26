---
title: "generate_series"
sidebar_label: "generate_series"
id: "generate_series"
description: "This macro implements a cross-database mechanism to generate an arbitrarily long list of numbers. Specify the maximum number you'd like in your list and it will create a 1-indexed SQL result set."
category: "SQL generators"
---

This macro implements a cross-database mechanism to generate an arbitrarily long list of numbers. Specify the maximum number you'd like in your list and it will create a 1-indexed SQL result set.

**Usage:**

```sql
{{ dbt_utils.generate_series(upper_bound=1000) }}
```
