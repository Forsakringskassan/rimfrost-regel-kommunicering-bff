package se.fk.github.kommuniceringbff.model;

/**
 * The backend declares handlaggningId as format: uuid. Validating it here gives the frontend a 400
 * instead of an opaque upstream error.
 */
public final class HandlaggningIdFormat
{
   public static final String UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

   private HandlaggningIdFormat()
   {
   }
}
