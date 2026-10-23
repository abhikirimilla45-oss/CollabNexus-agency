create table contact_messages (
    id bigint not null auto_increment,
    name varchar(120) not null,
    email varchar(180) not null,
    role enum ('BRAND','CREATOR','AGENCY','OTHER') not null,
    topic enum ('GENERAL','CAMPAIGNS','PAYMENTS','SECURITY','PRIVACY','PARTNERSHIP') not null,
    message varchar(4000) not null,
    status enum ('NEW','HANDLED') not null,
    created_at datetime(6) not null,
    handled_at datetime(6),
    handled_by bigint,
    primary key (id)
) engine=InnoDB;

create index idx_contact_created on contact_messages (created_at);
create index idx_contact_status on contact_messages (status, created_at);
