# Watermelon-Online

Watermelon-Online is the web application for the Watermelon database of openly available natural products structures.

Prior to any further action, a target folder needs to be generated, with the following command:

```
mvn package
```

To fire up a local version of it, you need to have Docker installed.


In the directory containing the 'docker-compose.yml' file run the following commands:


```
docker exec -it watermelon-mongo-db bash

mongo --port 27020

use WMNPOC
db.dropDatabase()
exit

cd mongodata/WMNPOC/WMNPOC/
mongorestore --port 27020 --db=WMNPOC --noIndexRestore .

mongo --port 27020
use WMNPOC

db.watermelonMolecule.createIndex( {inchi:"hashed"})
db.watermelonMolecule.createIndex( {inchikey:1})
db.watermelonMolecule.createIndex( {unique_smiles: "hashed"})
db.watermelonMolecule.createIndex( {absolute_smiles: "hashed"})
db.watermelonMolecule.createIndex( {original_smiles: "hashed"})
db.watermelonMolecule.createIndex( {molecular_formula:1})

db.runCommand(
  {
    createIndexes: 'watermelonMolecule',
    indexes: [
        {
            key: {
                alternativeName:"text", compoundName:"text", acf_id:"text", pass_bioactivity_searcheable:"text", chemicalTaxonomySearcheable:"text"
            },
            name: "superTextIndex",
      weights: { name:10, synonyms:5  }
        }

    ]
  }
)

db.watermelonMolecule.createIndex( { chemicalTaxonomyNPclassifierPathway : "hashed" } ) 
db.watermelonMolecule.createIndex( { chemicalTaxonomyNPclassifierSuperclass : "hashed" } ) 
db.watermelonMolecule.createIndex( { chemicalTaxonomyNPclassifierClass : "hashed" } )

db.watermelonMolecule.createIndex( {molecular_weight:1}) 
db.watermelonMolecule.createIndex( {fsp3:1})
db.watermelonMolecule.createIndex( {lipinskiRuleOf5Failures:1})
db.watermelonMolecule.createIndex( {heavy_atom_number:1})

db.watermelonMolecule.createIndex( {afc_id:1})

db.watermelonMolecule.createIndex( { pubchemBits : "hashed" } )

db.watermelonMolecule.createIndex( {deep_smiles: "hashed"})
db.watermelonMolecule.createIndex( { "pfCounts.bits" :1} )
db.watermelonMolecule.createIndex( { "pfCounts.count" : 1 })

exit
exit
```



# Rebuild the interface 

In case you want to rebuild the "watermelon-online" container only (because changes have been made to the front- or back-end) run the following:
```
docker-compose up -d --no-deps --build watermelon-online
```


