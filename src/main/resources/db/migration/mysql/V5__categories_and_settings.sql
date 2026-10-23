create table campaign_categories (
    active bit(1) not null,
    sort_order int not null,
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    name varchar(80) not null,
    slug varchar(80) not null,
    description varchar(500),
    version bigint,
    primary key (id)
) engine=InnoDB;

create unique index uk_campaign_category_name on campaign_categories (name);
create unique index uk_campaign_category_slug on campaign_categories (slug);
create index idx_campaign_category_active on campaign_categories (active, sort_order);

insert into campaign_categories (name, slug, description, active, sort_order, created_at, updated_at)
values ('Beauty & Skincare', 'beauty-skincare', 'Skincare, cosmetics, and personal care creators.', true, 10, current_timestamp(6), current_timestamp(6)),
       ('Fashion & Lifestyle', 'fashion-lifestyle', 'Fashion, styling, home, and everyday lifestyle content.', true, 20, current_timestamp(6), current_timestamp(6)),
       ('Travel & Hospitality', 'travel-hospitality', 'Destinations, hotels, and travel experiences.', true, 30, current_timestamp(6), current_timestamp(6)),
       ('Technology & Productivity', 'technology-productivity', 'Software, devices, and productivity workflows.', true, 40, current_timestamp(6), current_timestamp(6)),
       ('Food & Beverage', 'food-beverage', 'Recipes, restaurants, and packaged food or drink.', true, 50, current_timestamp(6), current_timestamp(6)),
       ('Health & Wellness', 'health-wellness', 'Fitness, nutrition, and wellbeing content.', true, 60, current_timestamp(6), current_timestamp(6)),
       ('Business & Finance', 'business-finance', 'Professional services, finance, and entrepreneurship.', true, 70, current_timestamp(6), current_timestamp(6)),
       ('Gaming & Entertainment', 'gaming-entertainment', 'Games, streaming, and entertainment formats.', true, 80, current_timestamp(6), current_timestamp(6)),
       ('Education', 'education', 'Courses, learning tools, and educational content.', true, 90, current_timestamp(6), current_timestamp(6)),
       ('Sustainability', 'sustainability', 'Sustainable products, circularity, and environmental topics.', true, 100, current_timestamp(6), current_timestamp(6));

create table platform_settings (
    id bigint not null auto_increment,
    setting_key varchar(80) not null,
    setting_value varchar(2000) not null,
    description varchar(500),
    updated_at datetime(6) not null,
    version bigint,
    primary key (id)
) engine=InnoDB;

create unique index uk_platform_setting_key on platform_settings (setting_key);

insert into platform_settings (setting_key, setting_value, description, updated_at)
values ('registration.open', 'true', 'Allow new brand and creator registrations.', current_timestamp(6)),
       ('marketplace.status', 'AVAILABLE', 'Public availability marker: AVAILABLE, LIMITED, or READ_ONLY.', current_timestamp(6)),
       ('default.currency', 'USD', 'Fallback three-letter currency code for new campaigns.', current_timestamp(6)),
       ('support.email', 'support@collabnexus.example', 'Public support contact shown to users.', current_timestamp(6));
