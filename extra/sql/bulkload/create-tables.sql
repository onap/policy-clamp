
    create table dictionary (
       name varchar(255) not null,
        created_by varchar(255),
        created_timestamp datetime(6) not null,
        updated_by varchar(255),
        updated_timestamp datetime(6) not null,
        dictionary_second_level integer,
        dictionary_type varchar(255),
        primary key (name)
    ) engine=InnoDB;

    create table dictionary_elements (
       name varchar(255) not null,
        created_by varchar(255),
        created_timestamp datetime(6) not null,
        updated_by varchar(255),
        updated_timestamp datetime(6) not null,
        description varchar(255),
        short_name varchar(255) not null,
        subdictionary_id varchar(255) not null,
        type varchar(255) not null,
        dictionary_id varchar(255),
        primary key (name)
    ) engine=InnoDB;

    create table hibernate_sequence (
       next_val bigint
    ) engine=InnoDB;

    insert into hibernate_sequence values ( 1 );

    create table loop_logs (
       id bigint not null,
        log_component varchar(255) not null,
        log_instant datetime(6) not null,
        log_type varchar(255) not null,
        message MEDIUMTEXT not null,
        loop_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table loop_templates (
       name varchar(255) not null,
        created_by varchar(255),
        created_timestamp datetime(6) not null,
        updated_by varchar(255),
        updated_timestamp datetime(6) not null,
        blueprint_yaml MEDIUMTEXT not null,
        maximum_instances_allowed integer,
        svg_representation MEDIUMTEXT,
        service_uuid varchar(255),
        primary key (name)
    ) engine=InnoDB;

    create table loops (
       name varchar(255) not null,
        created_by varchar(255),
        created_timestamp datetime(6) not null,
        updated_by varchar(255),
        updated_timestamp datetime(6) not null,
        blueprint_yaml MEDIUMTEXT not null,
        dcae_blueprint_id varchar(255),
        dcae_deployment_id varchar(255),
        dcae_deployment_status_url varchar(255),
        global_properties_json json,
        last_computed_state varchar(255) not null,
        svg_representation MEDIUMTEXT,
        loop_template_name varchar(255),
        service_uuid varchar(255),
        primary key (name)
    ) engine=InnoDB;

    create table loops_microservicepolicies (
       loop_id varchar(255) not null,
        microservicepolicy_id varchar(255) not null,
        primary key (loop_id, microservicepolicy_id)
    ) engine=InnoDB;

    create table micro_service_models (
       name varchar(255) not null,
        created_by varchar(255),
        created_timestamp datetime(6) not null,
        updated_by varchar(255),
        updated_timestamp datetime(6) not null,
        blueprint_yaml varchar(255) not null,
        policy_type varchar(255) not null,
        policy_model_type varchar(255),
        policy_model_version varchar(255),
        primary key (name)
    ) engine=InnoDB;

    create table micro_service_policies (
       name varchar(255) not null,
        created_by varchar(255),
        created_timestamp datetime(6) not null,
        updated_by varchar(255),
        updated_timestamp datetime(6) not null,
        context varchar(255),
        dcae_deployment_id varchar(255),
        dcae_deployment_status_url varchar(255),
        device_type_scope varchar(255),
        json_representation json not null,
        policy_model_type varchar(255) not null,
        policy_tosca MEDIUMTEXT not null,
        properties json,
        shared bit not null,
        micro_service_model_id varchar(255),
        primary key (name)
    ) engine=InnoDB;

    create table operational_policies (
       name varchar(255) not null,
        configurations_json json,
        json_representation json not null,
        loop_id varchar(255) not null,
        policy_model_type varchar(255),
        policy_model_version varchar(255),
        primary key (name)
    ) engine=InnoDB;

    create table policy_models (
       policy_model_type varchar(255) not null,
        version varchar(255) not null,
        created_by varchar(255),
        created_timestamp datetime(6) not null,
        updated_by varchar(255),
        updated_timestamp datetime(6) not null,
        policy_acronym varchar(255),
        policy_tosca MEDIUMTEXT,
        policy_variant varchar(255),
        primary key (policy_model_type, version)
    ) engine=InnoDB;

    create table services (
       service_uuid varchar(255) not null,
        name varchar(255) not null,
        resource_details json,
        service_details json,
        version varchar(255),
        primary key (service_uuid)
    ) engine=InnoDB;

    create table templates_microservicemodels (
       loop_template_name varchar(255) not null,
        micro_service_model_name varchar(255) not null,
        flow_order integer not null,
        primary key (loop_template_name, micro_service_model_name)
    ) engine=InnoDB;

    alter table dictionary_elements 
       add constraint UK_qxkrvsrhp26m60apfvxphpl3d unique (short_name);

    alter table dictionary_elements 
       add constraint FKn87bpgpm9i56w7uko585rbkgn 
       foreign key (dictionary_id) 
       references dictionary (name);

    alter table loop_logs 
       add constraint FK1j0cda46aickcaoxqoo34khg2 
       foreign key (loop_id) 
       references loops (name);

    alter table loop_templates 
       add constraint FKn692dk6281wvp1o95074uacn6 
       foreign key (service_uuid) 
       references services (service_uuid);

    alter table loops 
       add constraint FK844uwy82wt0l66jljkjqembpj 
       foreign key (loop_template_name) 
       references loop_templates (name);

    alter table loops 
       add constraint FK4b9wnqopxogwek014i1shqw7w 
       foreign key (service_uuid) 
       references services (service_uuid);

    alter table loops_microservicepolicies 
       add constraint FKem7tp1cdlpwe28av7ef91j1yl 
       foreign key (microservicepolicy_id) 
       references micro_service_policies (name);

    alter table loops_microservicepolicies 
       add constraint FKsvx91jekgdkfh34iaxtjfgebt 
       foreign key (loop_id) 
       references loops (name);

    alter table micro_service_models 
       add constraint FKlkcffpnuavcg65u5o4tr66902 
       foreign key (policy_model_type, policy_model_version) 
       references policy_models (policy_model_type, version);

    alter table micro_service_policies 
       add constraint FK5p7lipy9m2v7d4n3fvlclwse 
       foreign key (micro_service_model_id) 
       references micro_service_models (name);

    alter table operational_policies 
       add constraint FK1ddoggk9ni2bnqighv6ecmuwu 
       foreign key (loop_id) 
       references loops (name);

    alter table operational_policies 
       add constraint FKlsyhfkoqvkwj78ofepxhoctip 
       foreign key (policy_model_type, policy_model_version) 
       references policy_models (policy_model_type, version);

    alter table templates_microservicemodels 
       add constraint FKq2gqg5q9jrkx8voosn7x5plqo 
       foreign key (loop_template_name) 
       references loop_templates (name);

    alter table templates_microservicemodels 
       add constraint FKphn3m81suxavmj9c4u06cchju 
       foreign key (micro_service_model_name) 
       references micro_service_models (name);
