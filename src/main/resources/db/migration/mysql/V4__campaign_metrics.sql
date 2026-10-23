create table campaign_metrics (
    campaign_id bigint not null,
    recorded_on date not null,
    reach_count bigint,
    impressions_count bigint,
    clicks_count bigint,
    conversions_count bigint,
    spend decimal(12,2),
    revenue decimal(12,2),
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    source varchar(120),
    notes varchar(2000),
    version bigint,
    primary key (id)
) engine=InnoDB;

create index idx_campaign_metric_campaign on campaign_metrics (campaign_id, recorded_on);

alter table campaign_metrics
    add constraint fk_campaign_metric_campaign foreign key (campaign_id) references campaigns (id);
