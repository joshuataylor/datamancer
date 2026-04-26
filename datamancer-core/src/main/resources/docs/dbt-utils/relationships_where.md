---
title: "relationships_where"
sidebar_label: "relationships_where"
id: "relationships_where"
description: "Asserts the referential integrity between two relations (same as the core relationships assertions) with an added predicate to filter out some rows from the test. This is useful to exclude records ..."
category: "Generic Tests"
---

Asserts the referential integrity between two relations (same as the core relationships assertions) with an added predicate to filter out some rows from the test. This is useful to exclude records such as test entities, rows created in the last X minutes/hours to account for temporary gaps due to ETL limitations, etc.

**Usage:**

```yaml
version: 2

models:
  - name: model_name
    columns:
      - name: id
        tests:
          - dbt_utils.relationships_where:
              arguments:
                to: ref('other_model_name')
                field: client_id
                from_condition: id <> '4ca448b8-24bf-4b88-96c6-b1609499c38b'
                to_condition: created_date >= '2020-01-01'
```
