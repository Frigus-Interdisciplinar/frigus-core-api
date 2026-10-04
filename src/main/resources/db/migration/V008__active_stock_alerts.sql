CREATE OR REPLACE FUNCTION trg_fn_notify_commercial_stock_level() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE v_group_id UUID; v_type notification_type_enum; v_title VARCHAR;
BEGIN
 SELECT s.group_id INTO v_group_id FROM stocks s JOIN groups g ON g.id=s.group_id
 JOIN users u ON u.id=g.owner_id WHERE s.id=NEW.stock_id AND s.deleted_at IS NULL
 AND g.deleted_at IS NULL AND u.account_type IN ('DOMESTIC','COMMERCIAL');
 IF v_group_id IS NULL OR NEW.deleted_at IS NOT NULL THEN RETURN NEW; END IF;
 IF NEW.quantity=0 AND (TG_OP='INSERT' OR OLD.quantity IS DISTINCT FROM 0) THEN
  v_type:='OUT_OF_STOCK'; v_title:='Estoque zerado';
 ELSIF NEW.minimal_quantity IS NOT NULL AND NEW.quantity>0 AND NEW.quantity<=NEW.minimal_quantity
 AND (TG_OP='INSERT' OR OLD.quantity>COALESCE(OLD.minimal_quantity,-1)
 OR OLD.minimal_quantity IS DISTINCT FROM NEW.minimal_quantity) THEN
  v_type:='LOW_STOCK'; v_title:='Produto com pouca quantidade no estoque';
 ELSE RETURN NEW; END IF;
 INSERT INTO notifications(recipient_user_id,group_id,type,title,body,reference_id)
 SELECT ug.user_id,v_group_id,v_type,v_title,p.name || ': quantidade atual ' || NEW.quantity,NEW.id::text
 FROM user_groups ug CROSS JOIN products p WHERE ug.group_id=v_group_id AND p.id=NEW.product_id;
 RETURN NEW;
END $$;
