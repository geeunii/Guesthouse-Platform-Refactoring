create index idx_accommodation_status_approval
    on accommodation (accommodation_status, approval_status);

create index idx_accommodation_name
    on accommodation (accommodations_name);

create index idx_accommodation_location
    on accommodation (city, district, township);
