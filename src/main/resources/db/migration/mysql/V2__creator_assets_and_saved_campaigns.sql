alter table creator_profiles add column avatar_url varchar(500);
alter table creator_profiles add column followers_count bigint;
alter table creator_profiles add column engagement_rate decimal(5,2);
alter table creator_profiles add column audience_countries varchar(255);

create table creator_social_channels (
    creator_id bigint not null,
    followers_count bigint,
    engagement_rate decimal(5,2),
    verified bit(1) not null,
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    handle varchar(120) not null,
    platform varchar(60) not null,
    url varchar(500),
    primary key (id)
) engine=InnoDB;

create table creator_portfolio_items (
    creator_id bigint not null,
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    title varchar(160) not null,
    url varchar(500) not null,
    description varchar(1000),
    primary key (id)
) engine=InnoDB;

create table saved_campaigns (
    campaign_id bigint not null,
    creator_id bigint not null,
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

alter table saved_campaigns
    add constraint uk_saved_campaign_creator unique (creator_id, campaign_id);

create index idx_social_channel_creator on creator_social_channels (creator_id);
create index idx_portfolio_item_creator on creator_portfolio_items (creator_id);
create index idx_saved_campaign_creator on saved_campaigns (creator_id, created_at);
create index idx_saved_campaign_campaign on saved_campaigns (campaign_id);

alter table creator_social_channels
    add constraint fk_social_channel_creator foreign key (creator_id) references creator_profiles (id);
alter table creator_portfolio_items
    add constraint fk_portfolio_item_creator foreign key (creator_id) references creator_profiles (id);
alter table saved_campaigns
    add constraint fk_saved_campaign_creator foreign key (creator_id) references creator_profiles (id);
alter table saved_campaigns
    add constraint fk_saved_campaign_campaign foreign key (campaign_id) references campaigns (id);
