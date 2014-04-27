package org.squirrelsql.session.schemainfo;

import org.squirrelsql.aliases.AliasPropertiesDecorator;
import org.squirrelsql.aliases.dbconnector.DbConnectorResult;
import org.squirrelsql.services.CaseInsensitiveString;
import org.squirrelsql.session.ColumnInfo;
import org.squirrelsql.session.ProcedureInfo;
import org.squirrelsql.session.TableInfo;
import org.squirrelsql.session.UDTInfo;
import org.squirrelsql.session.completion.TableTypes;
import org.squirrelsql.session.objecttree.TableDetailsReader;
import org.squirrelsql.table.TableLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SchemaCache
{
   private final SchemaCacheConfig _schemaCacheConfig;

   private DatabaseStructure _databaseStructure;

   private TableLoader _dataTypes;
   private TableLoader _dataBaseMetadData;
   private TableLoader _numericFunctions;
   private TableLoader _stringFunctions;
   private TableLoader _systemFunctions;
   private TableLoader _timeDateFunctions;
   private TableLoader _keywords;

   private HashMap<StructItemTableType, ArrayList<TableInfo>> _tableInfos = new HashMap<>();

   private HashMap<FullyQualifiedTableName, ArrayList<TableInfo>> _tableInfosByFullyQualifiedName = new HashMap<>();
   private HashMap<SchemaQualifiedTableName, ArrayList<TableInfo>> _tableInfosBySchemaQualifiedName = new HashMap<>();
   private HashMap<String, ArrayList<TableInfo>> _tableInfosBySimpleName = new HashMap<>();

   private HashMap<StructItemProcedureType, ArrayList<ProcedureInfo>> _procedureInfos = new HashMap<>();
   private HashMap<StructItemUDTType, ArrayList<UDTInfo>> _udtInfos = new HashMap<>();
   private DbConnectorResult _dbConnectorResult;


   private CaseInsensitiveCache _caseInsensitiveCache = new CaseInsensitiveCache();

   public SchemaCache(DbConnectorResult dbConnectorResult, SchemaCacheConfig schemaCacheConfig, DatabaseStructure databaseStructure)
   {
      _dbConnectorResult = dbConnectorResult;
      _schemaCacheConfig = schemaCacheConfig;
      _databaseStructure = databaseStructure;
   }

   public void load()
   {
      if(_schemaCacheConfig.shouldNotLoad())
      {
         return;
      }


      _dataBaseMetadData = DataBaseMetaDataLoader.loadMetaData(_dbConnectorResult.getAlias(), _dbConnectorResult.getSQLConnection());
      _dataTypes = DataTypesLoader.loadTypes(_dbConnectorResult.getSQLConnection());

      _numericFunctions = DataBaseMetaDataLoader.loadNumericFunctions(_dbConnectorResult.getSQLConnection());
      _numericFunctions.getRows().forEach(e -> _caseInsensitiveCache.addProc((String) e.get(0)));

      _stringFunctions = DataBaseMetaDataLoader.loadStringFunctions(_dbConnectorResult.getSQLConnection());
      _stringFunctions.getRows().forEach(e ->  _caseInsensitiveCache.addProc((String) e.get(0)));

      _systemFunctions = DataBaseMetaDataLoader.loadSystemFunctions(_dbConnectorResult.getSQLConnection());
      _systemFunctions.getRows().forEach(e ->  _caseInsensitiveCache.addProc((String) e.get(0)));

      _timeDateFunctions = DataBaseMetaDataLoader.loadTimeDateFunctions(_dbConnectorResult.getSQLConnection());
      _timeDateFunctions.getRows().forEach(e ->  _caseInsensitiveCache.addProc((String) e.get(0)));

      _keywords = DataBaseMetaDataLoader.loadKeyWords(_dbConnectorResult.getSQLConnection());
      _keywords.getRows().forEach(e ->  _caseInsensitiveCache.addKeyword((String) e.get(0)));

      for (String keyWord : DefaultKeywords.KEY_WORDS)
      {
         _caseInsensitiveCache.addKeyword(keyWord);
      }

      ArrayList<StructItem> leaves = _databaseStructure.getLeaves();

      for (StructItem leaf : leaves)
      {
         if(leaf instanceof StructItemTableType)
         {
            StructItemTableType buf = (StructItemTableType) leaf;
            if (buf.shouldLoad(_schemaCacheConfig))
            {
               ArrayList<TableInfo> tableInfos = _dbConnectorResult.getSQLConnection().getTableInfos(buf.getCatalog(), buf.getSchema(), buf.getType());
               _tableInfos.put(buf, tableInfos);


               for (TableInfo tableInfo : tableInfos)
               {
                  ArrayList<TableInfo> arr;

                  FullyQualifiedTableName fullyQualifiedTableName = new FullyQualifiedTableName(buf.getCatalog(), buf.getSchema(), tableInfo.getName());
                  arr = _tableInfosByFullyQualifiedName.get(fullyQualifiedTableName);

                  if(null == arr)
                  {
                     arr = new ArrayList<>();
                     _tableInfosByFullyQualifiedName.put(fullyQualifiedTableName, arr);
                  }
                  arr.add(tableInfo);

                  SchemaQualifiedTableName schemaQualifiedTableName = new SchemaQualifiedTableName(buf.getSchema(), tableInfo.getName());
                  arr = _tableInfosByFullyQualifiedName.get(schemaQualifiedTableName);

                  if(null == arr)
                  {
                     arr = new ArrayList<>();
                     _tableInfosBySchemaQualifiedName.put(schemaQualifiedTableName, arr);
                  }
                  arr.add(tableInfo);

                  arr = _tableInfosBySimpleName.get(tableInfo.getName());

                  if(null == arr)
                  {
                     arr = new ArrayList<>();
                     _tableInfosBySimpleName.put(tableInfo.getName(), arr);
                  }
                  arr.add(tableInfo);

                  _caseInsensitiveCache.addTable(tableInfo.getName());

               }

            }
         }
         else if(leaf instanceof StructItemProcedureType)
         {
            StructItemProcedureType buf = (StructItemProcedureType) leaf;
            if (buf.shouldLoad(_schemaCacheConfig))
            {
               ArrayList<ProcedureInfo> procedureInfos = _dbConnectorResult.getSQLConnection().getProcedureInfos(buf.getCatalog(), buf.getSchema());
               _procedureInfos.put(buf, procedureInfos);

               for (ProcedureInfo procedureInfo : procedureInfos)
               {
                  _caseInsensitiveCache.addProc(procedureInfo.getName());
               }
            }
         }
         else if(leaf instanceof StructItemUDTType)
         {
            StructItemUDTType buf = (StructItemUDTType) leaf;
            if (buf.shouldLoad(_schemaCacheConfig))
            {
               _udtInfos.put(buf, _dbConnectorResult.getSQLConnection().getUDTInfos(buf.getCatalog(), buf.getSchema()));
            }
         }
      }
   }

   public DatabaseStructure getDataBaseStructure()
   {
      return _databaseStructure;
   }

   public SchemaCacheConfig getSchemaCacheConfig()
   {
      return _schemaCacheConfig;
   }

   public ArrayList<TableInfo> getTableInfosExact(String catalog, String schema, String tableType)
   {
      return convertNullToArray(_tableInfos.get(new StructItemTableType(tableType, catalog, schema)));
   }

   public ArrayList<TableInfo> getTableInfosMatching(String catalog, String schema, TableTypes[] allowedTypes)
   {
      ArrayList<TableInfo> ret = new ArrayList<>();

      for (StructItemTableType structItemTableType : _tableInfos.keySet())
      {
         if(structItemTableType.matchesRespectNull(catalog, schema, allowedTypes))
         {
            ret.addAll(_tableInfos.get(structItemTableType));
         }
      }

      return ret;
   }

   public ArrayList<ProcedureInfo> getProcedureInfosExact(String catalog, String schema)
   {
      return convertNullToArray(_procedureInfos.get(new StructItemProcedureType(catalog, schema)));
   }

   public ArrayList<ProcedureInfo> getProcedureInfosMatching(String catalog, String schema)
   {
      ArrayList<ProcedureInfo> ret = new ArrayList<>();

      for (StructItemProcedureType structItemProcedureType : _procedureInfos.keySet())
      {
         if(structItemProcedureType.matchesRespectNull(catalog, schema))
         {
            ret.addAll(_procedureInfos.get(structItemProcedureType));
         }
      }

      return ret;
   }

   public ArrayList<UDTInfo> getUDTInfosExact(String catalog, String schema)
   {
      return convertNullToArray(_udtInfos.get(new StructItemUDTType(catalog, schema)));
   }


   public ArrayList<UDTInfo> getUDTInfosMatching(String catalog, String schema)
   {
      ArrayList<UDTInfo> ret = new ArrayList<>();

      for (StructItemUDTType structItemUDTType : _udtInfos.keySet())
      {
         if(structItemUDTType.matchesRespectNull(catalog, schema))
         {
            ret.addAll(_udtInfos.get(structItemUDTType));
         }
      }

      return ret;
   }

   private <T> ArrayList<T> convertNullToArray(ArrayList<T> arr)
   {
      if (null == arr)
      {
         return new ArrayList<>();
      }

      return arr;
   }

   public TableLoader getTypes()
   {
      return _dataTypes;
   }

   public TableLoader getDatabaseMetaData()
   {
      return _dataBaseMetadData;
   }

   public TableLoader getNumericFunctions()
   {
      return _numericFunctions;
   }

   public TableLoader getStringFunctions()
   {
      return _stringFunctions;
   }

   public TableLoader getSystemFunctions()
   {
      return _systemFunctions;
   }

   public TableLoader getTimeDateFunctions()
   {
      return _timeDateFunctions;
   }

   public TableLoader getKeywords()
   {
      return _keywords;
   }

   public ArrayList<StructItemCatalog> getCatalogs()
   {
      return _databaseStructure.getCatalogs();
   }

   public ArrayList<StructItemSchema> getSchemas()
   {
      return convertNullToArray(_databaseStructure.getSchemas());
   }

   public ArrayList<String> getAllFunctions()
   {
      ArrayList<String> ret = new ArrayList<>();

      ret.addAll(_stringFunctions.getCellsAsString(0));
      ret.addAll(_numericFunctions.getCellsAsString(0));
      ret.addAll(_timeDateFunctions.getCellsAsString(0));
      ret.addAll(_systemFunctions.getCellsAsString(0));

      return ret;
   }

   public StructItemCatalog getCatalogByName(String catalogName)
   {
      return _databaseStructure.getCatalogByName(catalogName);
   }

   public ArrayList<StructItemSchema> getSchemasByName(String schemaName)
   {
      return convertNullToArray(_databaseStructure.getSchemasByName(schemaName));
   }

   public ArrayList<StructItemSchema> getSchemaByNameAsArray(String catalogName, String schemaName)
   {
      return convertNullToArray(_databaseStructure.getSchemaByNameAsArray(catalogName, schemaName));
   }

   public ArrayList<TableInfo> getTablesByFullyQualifiedName(String catalog, String schema, String tableName)
   {
      return convertNullToArray(_tableInfosByFullyQualifiedName.get(new FullyQualifiedTableName(catalog, schema, tableName)));
   }

   public ArrayList<TableInfo> getTablesBySchemaQualifiedName(String schema, String tableName)
   {
      return convertNullToArray(_tableInfosBySchemaQualifiedName.get(new SchemaQualifiedTableName(schema, tableName)));
   }

   public ArrayList<TableInfo> getTablesBySimpleName(String tableName)
   {
      return convertNullToArray(_tableInfosBySimpleName.get(tableName));
   }



   public String[] getDefaultKeywords()
   {
      return DefaultKeywords.KEY_WORDS;
   }

   public TableLoader getColumnsAsTableLoader(TableInfo table)
   {
      initCols(table);
      return table.getColumnsAsTableLoader();
   }

   public ArrayList<ColumnInfo> getColumns(TableInfo table)
   {
      initCols(table);
      return table.getColumns();
   }

   private void initCols(TableInfo table)
   {
      if(null == table.getColumnsAsTableLoader())
      {
         table.setColumnsAsTableLoader(TableDetailsReader.readColumns(table, _dbConnectorResult));
      }
   }

   public AliasPropertiesDecorator getAliasPropertiesDecorator()
   {
      return _schemaCacheConfig.getAliasPropertiesDecorator();
   }

   public boolean isTable(char[] buffer, int offset, int len)
   {
      return _caseInsensitiveCache.isTable(buffer, offset, len);
   }

   public boolean isProcedure(char[] buffer, int offset, int len)
   {
      return _caseInsensitiveCache.isProcedure(buffer, offset, len);
   }

   public boolean isKeyword(char[] buffer, int offset, int len)
   {
      return _caseInsensitiveCache.isKeyword(buffer, offset, len);
   }
}
