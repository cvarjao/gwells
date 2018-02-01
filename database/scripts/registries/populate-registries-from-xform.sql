


                                  ,

INSERT INTO registries_organization (              
 name                  
,street_address        
,city                  
,postal_code           
,main_tel              
,fax_tel               
,website_url           
,certificate_authority 
,province_state_guid
,org_guid   
) AS SELECT
 distinct on (companyname) companyname
,companyaddress
,companycity
,companypostalcode
,companyphone
,companyfax
,null
,false
,prov.province_state_guid
,gen_random_uuid()
from xform_registries_drillers_reg xform
    ,gwells_province_state prov
where companyname is not null
and   prov.code = xform.companyprov
order by companyname;


/*
DROP FUNCTION IF EXISTS gwells_populate_well();

CREATE OR REPLACE FUNCTION gwells_populate_well() RETURNS void AS $$
DECLARE
  row_count integer;
BEGIN
  raise notice '... importing xform into the gwells_well table';

  INSERT INTO gwells_well (
    well_tag_number                    ,
    well_guid                          ,
    owner_full_name                    ,
    owner_mailing_address              ,
    owner_city                         ,
    owner_postal_code                  ,
    street_address                     ,
    city                               ,
    legal_lot                          ,
    legal_plan                         ,
    legal_district_lot                 ,
    legal_block                        ,
    legal_section                      ,
    legal_township                     ,
    legal_range                        ,
    land_district_guid                 ,
    legal_pid                          ,
    well_location_description          ,
    identification_plate_number        ,
    diameter                           ,
    total_depth_drilled                ,
    finished_well_depth                ,
    static_water_level                 ,
    well_cap_type                      ,
    well_disinfected                   ,
    well_yield                         ,
    intended_water_use_guid            ,
    province_state_guid                ,
    well_class_guid                    ,
    well_subclass_guid                 ,
    well_yield_unit_guid               ,
    latitude                           ,
    longitude                          ,
    ground_elevation                   ,
    well_orientation                   ,
    other_drilling_method              ,
    drilling_method_guid               ,
    ground_elevation_method_guid       ,
    when_created                       ,
    when_updated                       ,
    who_created                        ,
    who_updated                        ,
    surface_seal_length                ,
    surface_seal_thickness             ,
    surface_seal_method_guid           ,
    surface_seal_material_guid         ,
    backfill_type                      ,
    backfill_depth                     ,
    liner_material_guid                ,
    well_status_guid                   ,
    observation_well_number            ,
    observation_well_status_guid       ,
    licenced_status_guid               ,
    other_screen_bottom                ,
    other_screen_material              ,
    development_notes                  ,
    water_quality_colour               ,
    water_quality_odour                ,
    alternative_specs_submitted        ,
    construction_start_date            ,
    construction_end_date              ,
    alteration_start_date              ,
    alteration_end_date                ,
    decommission_start_date            ,
    decommission_end_date              ,
    drilling_company_guid              ,
    final_casing_stick_up              ,
    artesian_flow                      ,
    artesian_pressure                  ,
    bedrock_depth                      ,
    water_supply_system_name           ,
    water_supply_system_well_name      ,
    well_identification_plate_attached ,
    ems                                ,
    screen_intake_method_guid          ,
    screen_type_guid                   ,
    screen_material_guid               ,
    screen_opening_guid                ,
    screen_bottom_guid                 ,
    utm_zone_code                      ,
    utm_northing                       ,
    utm_easting                        ,
    utm_accuracy_code                  ,
    bcgs_id                            ,
    development_method_guid            ,
    development_hours                  ,
    decommission_reason                ,
    decommission_method_guid           ,
    sealant_material                   ,
    backfill_material                  ,
    decommission_details               ,
    comments
    )
  SELECT
  	xform.well_tag_number                          ,
  	gen_random_uuid()                              ,
  	COALESCE(xform.owner_full_name,' ')            ,
  	COALESCE(xform.owner_mailing_address, ' ')     ,
  	COALESCE(xform.owner_city, ' ')                ,
  	COALESCE(xform.owner_postal_code , ' ')        ,
  	COALESCE(xform.street_address    , ' ')        ,
  	COALESCE(xform.city              , ' ')        ,
  	COALESCE(xform.legal_lot         , ' ')        ,
  	COALESCE(xform.legal_plan        , ' ')        ,
  	COALESCE(xform.legal_district_lot, ' ')        ,
  	COALESCE(xform.legal_block       , ' ')        ,
  	COALESCE(xform.legal_section     , ' ')        ,
  	COALESCE(xform.legal_township    , ' ')        ,
  	COALESCE(xform.legal_range       , ' ')        ,
    xform.land_district_guid                       ,
  	xform.legal_pid                                ,
  	COALESCE(xform.well_location_description,' ')  ,
  	xform.identification_plate_number              ,
  	COALESCE(xform.diameter, ' ')                  ,
  	xform.total_depth_drilled                      ,
  	xform.finished_well_depth                      ,
    xform.static_water_level                       ,
    xform.well_cap_type                            ,
    xform.well_disinfected                         ,
  	xform.well_yield                               ,
  	xform.intended_water_use_guid                  ,
  	xform.province_state_guid                      ,
  	xform.well_class_guid                          ,
  	xform.well_subclass_guid                       ,
  	xform.well_yield_unit_guid                     ,
  	xform.latitude                                 ,
  	xform.longitude                                ,
  	xform.ground_elevation                         ,
  	xform.well_orientation                         ,
  	NULL                                           ,
  	xform.drilling_method_guid                     ,
  	xform.ground_elevation_method_guid             ,
  	xform.when_created                             ,
  	xform.when_updated                             ,
  	xform.who_created                              ,
  	xform.who_updated                              ,
  	xform.surface_seal_length                      ,
  	xform.surface_seal_thickness                   ,
  	xform.surface_seal_method_guid                 ,
    xform.surface_seal_material_guid               ,
    xform.backfill_type                            ,
    xform.backfill_depth                           ,
    xform.liner_material_guid                      ,
  	xform.well_status_guid                         ,
    xform.observation_well_number                  ,
    xform.observation_well_status_guid             ,
  	xform.licenced_status_guid                     ,
  	''                                             ,
  	''                                             ,
  	''                                             ,
  	''                                             ,
  	''                                             ,
  	false                                          ,
  	xform.construction_start_date                  ,
  	xform.construction_end_date                    ,
  	xform.alteration_start_date                    ,
  	xform.alteration_end_date                      ,
  	xform.decommission_start_date                  ,
  	xform.decommission_end_date                    ,
  	xform.drilling_company_guid                    ,
    xform.final_casing_stick_up                    ,
    xform.artesian_flow                            ,
    xform.artesian_pressure                        ,
    xform.bedrock_depth                            ,
    xform.water_supply_system_name                 ,
    xform.water_supply_system_well_name            ,
    xform.well_identification_plate_attached       ,
    xform.ems                                      ,
    xform.screen_intake_method_guid                ,
    xform.screen_type_guid                         ,
    xform.screen_material_guid                     ,
    xform.screen_opening_guid                      ,
    xform.screen_bottom_guid                       ,
    xform.utm_zone_code                            ,
    xform.utm_northing                             ,
    xform.utm_easting                              ,
    xform.utm_accuracy_code                        ,
  	xform.bcgs_id                                  ,
    xform.development_method_guid                  ,
    xform.development_duration                     ,
    xform.decommission_reason                      ,
    xform.decommission_method_guid                 ,
    xform.sealant_material                         ,
    xform.backfill_material                        ,
    xform.decommission_details                     ,
    xform.comments
  FROM xform_gwells_well xform;

  raise notice '...xform data imported into the gwells_well table';
  SELECT count(*) from gwells_well into row_count;
  raise notice '% rows loaded into the gwells_well table',  row_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION gwells_populate_well () IS 'Transfer from local XFORM ETL table into gwells_well.';
*/