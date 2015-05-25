package transaction.tables;

import java.io.Serializable;

public class OperationPair implements Serializable{
   private String table;
   private String key;

   public OperationPair(String table, String key){
      this.table = table;
      this.key = key;
   }

   public String getTable(){
      return table;
   }

   public String getKey(){
      return key;
   }
}
