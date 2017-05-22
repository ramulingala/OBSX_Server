ALTER TABLE `b_country_currency` 
DROP COLUMN `country_ISD`,
DROP COLUMN `country`,
ADD COLUMN `valid_from` DATE NULL AFTER `conversion_rate`,
ADD COLUMN `valid_to` DATE NULL AFTER `valid_from`,
DROP INDEX `country_ISD_UNIQUE` ,
DROP INDEX `country_key` , RENAME TO `b_country_exchange` ;
