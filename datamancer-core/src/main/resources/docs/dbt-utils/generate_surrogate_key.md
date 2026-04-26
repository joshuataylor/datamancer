---
title: "generate_surrogate_key"
sidebar_label: "generate_surrogate_key"
id: "generate_surrogate_key"
description: "This macro implements a cross-database way to generate a hashed surrogate key using the fields specified."
category: "SQL generators"
---

This macro implements a cross-database way to generate a hashed surrogate key using the fields specified.

**Usage:**

```sql
{{ dbt_utils.generate_surrogate_key(['field_a', 'field_b'[,...]]) }}
```

A precursor to this macro, `surrogate_key()`, treated nulls and blanks strings the same. If you need to enable this incorrect behaviour for backward compatibility reasons, add the following variable to your `dbt_project.yml`: 

```yaml
#dbt_project.yml
vars:
  surrogate_key_treat_nulls_as_empty_strings: true #turn on legacy behaviour
```
