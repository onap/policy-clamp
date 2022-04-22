
    create table policy_models (
       policy_model_type varchar(255) not null,
        version varchar(255) not null,
        created_by varchar(255),
        created_timestamp datetime(6) not null,
        updated_by varchar(255),
        updated_timestamp datetime(6) not null,
        policy_acronym varchar(255),
        policy_tosca MEDIUMTEXT,
        policy_pdp_group json,
        primary key (policy_model_type, version)
    ) engine=InnoDB;
