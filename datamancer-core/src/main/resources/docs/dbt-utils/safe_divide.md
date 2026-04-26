---
title: "safe_divide"
sidebar_label: "safe_divide"
id: "safe_divide"
description: "This macro performs division but returns null if the denominator is 0."
category: "SQL generators"
---

This macro performs division but returns null if the denominator is 0. 

__Args__:

 * `numerator` (required): The number or SQL expression you want to divide.
 * `denominator` (required): The number or SQL expression you want to divide by.

**Usage:**

```sql
{{ dbt_utils.safe_divide('numerator', 'denominator') }}
```
