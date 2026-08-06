# MeethybridHub — Use Cases

> Viewable natively on GitHub. Editable draw.io version: [`06-use-case-diagram.drawio`](../06-use-case-diagram.drawio)
>
> Mermaid has no native use-case shape, so this is drawn as a flowchart with the same semantics: actors on the left, use cases inside the system boundary.

```mermaid
flowchart LR
    Guest["👤 Guest"]:::guest
    User["👤 User<br/>(Customer)"]:::user
    Owner["👤 Store Owner"]:::owner
    Admin["👤 Admin"]:::admin

    subgraph Boundary["MeethybridHub API — /api/v1"]
        direction TB
        UC1(["Register account"])
        UC2(["Login"])
        UC3(["Verify email (token)"])
        UC4(["Reset password"])
        UC5(["Refresh tokens"])
        UC6(["View / update profile"])
        UC7(["Change password"])
        UC8(["Delete account (soft)"])
        UC9(["Resend verification"])
        UC10(["Create store"])
        UC11(["View my store (tenant-scoped)"])
        UC12(["Manage domains"])
        UC13(["List / view users"])
        UC14(["Change roles"])
        UC15(["Suspend / activate user"])
        UC16(["Delete user"])
    end

    Guest --> UC1
    Guest --> UC2
    User --> UC2
    User --> UC3
    User --> UC6
    User --> UC7
    User --> UC8
    User --> UC9
    User --> UC5
    Owner --> UC10
    Owner --> UC11
    Owner --> UC12
    Admin --> UC13
    Admin --> UC14
    Admin --> UC15
    Admin --> UC16

    UC9 -. «extend» .-> UC3

    classDef guest fill:#fff2cc,stroke:#d6b656,stroke-width:2px
    classDef user fill:#dae8fc,stroke:#6c8ebf,stroke-width:2px
    classDef owner fill:#d5e8d4,stroke:#82b366,stroke-width:2px
    classDef admin fill:#f8cecc,stroke:#b85450,stroke-width:2px
```
