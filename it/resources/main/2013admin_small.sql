Insert into GEOGRAPHIC_AREA_HIERARCHY (GEOGRAPHIC_AREA_HIERARCHY) values ('2013ADMIN') on conflict do nothing;

Insert into GEOGRAPHIC_AREA (GEOGRAPHIC_AREA_ID, NAME, EXT_CODE, GEOGRAPHIC_AREA_HIERARCHY, GEOGRAPHIC_AREA_TYPE, GEOGRAPHIC_LEVEL_TYPE, REL_GEOGRAPHIC_AREA_ID) values (2000001,'United Kingdom','K02000001','2013ADMIN','UK','UK',null) on conflict do nothing;
Insert into GEOGRAPHIC_AREA (GEOGRAPHIC_AREA_ID, NAME, EXT_CODE, GEOGRAPHIC_AREA_HIERARCHY, GEOGRAPHIC_AREA_TYPE, GEOGRAPHIC_LEVEL_TYPE, REL_GEOGRAPHIC_AREA_ID) values (2000002,'Great Britain','K03000001','2013ADMIN','GB','GB',2000001) on conflict do nothing;
Insert into GEOGRAPHIC_AREA (GEOGRAPHIC_AREA_ID, NAME, EXT_CODE, GEOGRAPHIC_AREA_HIERARCHY, GEOGRAPHIC_AREA_TYPE, GEOGRAPHIC_LEVEL_TYPE, REL_GEOGRAPHIC_AREA_ID) values (2000003,'England and Wales','K04000001','2013ADMIN','EW','EW',2000001) on conflict do nothing;