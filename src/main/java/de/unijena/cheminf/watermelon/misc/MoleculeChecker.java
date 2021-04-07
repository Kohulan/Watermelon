package de.unijena.cheminf.watermelon.misc;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
public class MoleculeChecker {

    private static final int MIN_HEAVY_ATOM_COUNT = 5;
    private static final int MAX_HEAVY_ATOM_COUNT = 210;



    private final String[] check = {"C", "H", "N", "O", "P", "S", "Cl", "F", "As", "Se", "Br", "I", "B", "Na", "Si", "K", "Fe", "Mg", "Zn", "Ca", "Mo", "Mn", "Cu", "Cd"};
    private final HashSet<String> symbols2Check = new HashSet<String>(Arrays.asList(check));

    private final String[] forbiddenInchiKeys = {"OOHPORRAEMMMCX-UHFFFAOYSA-N", "ATSPGPYEGAPMOB-UHFFFAOYSA-N"};
    private final HashSet<String> inchis2Check = new HashSet<String>(Arrays.asList(forbiddenInchiKeys));



    MoleculeConnectivityChecker mcc;



    public IAtomContainer checkMolecule(IAtomContainer molecule){

        IAtomContainer oriMol = molecule;


        mcc = BeanUtil.getBean(MoleculeConnectivityChecker.class);

        SmilesGenerator sg = new SmilesGenerator(SmiFlavor.Absolute);
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());

        if(!containsStrangeElements(molecule)) {

            /**
             * Checking for connectivity and selecting the biggest component
             */

            List<IAtomContainer> listAC = mcc.checkConnectivity(molecule);
            if( listAC.size()>=1 ){
                IAtomContainer biggestComponent = listAC.get(0);
                for(IAtomContainer partac : listAC){
                    if(partac.getAtomCount()>biggestComponent.getAtomCount()){
                        biggestComponent = partac;
                    }
                }
                molecule = biggestComponent;

                int nbheavyatoms = 0;
                for(IAtom a : molecule.atoms()){
                    if(!a.getSymbol().equals("H")){
                        nbheavyatoms++;
                    }
                }
                if(nbheavyatoms<= MIN_HEAVY_ATOM_COUNT || nbheavyatoms>=MAX_HEAVY_ATOM_COUNT){
                    return null;
                }
            }
            else{

                return null;
            }



            // check ID

            if (molecule.getID() == "" || molecule.getID() == null) {
                for (Object p : molecule.getProperties().keySet()) {

                    if (p.toString().toLowerCase().contains("id")) {
                        molecule.setID(molecule.getProperty(p.toString()));

                    }

                }
                if (molecule.getID() == "" || molecule.getID() == null) {
                    molecule.setID(molecule.getProperty("MOL_NUMBER_IN_FILE"));
                    //this.molecule.setProperty("ID", this.molecule.getProperty("MOL_NUMBER_IN_FILE"));
                }


            }

            Map<Object, Object> properties = molecule.getProperties();
            String id = molecule.getID();
            oriMol = molecule;




            //Normalizing the ionization states

            //python3 GetParentSourceNP.py "C[C@H](O)c1ccccc1"

            try {
                //String command = "evaluate -e majorMicrospecies('7.4') "+sg.create(molecule);
                String command = "python3 GetParentSourceNP.py "+sg.create(molecule);
                Process process = Runtime.getRuntime().exec(command);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String correctSmi = line.replace("\n", "");
                    if (!line.contains("Normalizer") && !line.contains("charge") && !line.equals("") ){

                        System.out.println(correctSmi);

                    try {
                        molecule = sp.parseSmiles(correctSmi);
                        molecule.setProperties(properties);
                        molecule.setID(id);
                    } catch (CDKException | IllegalArgumentException e) {
                        e.printStackTrace();

                        molecule = oriMol;
                    }
                }
                }

                reader.close();


            }catch(Exception e){
                System.out.println("Couldn't start Major Microscpecies calculation: something went wrong");
                e.printStackTrace();
                molecule = oriMol;
            }






            //ElectronDonation model = ElectronDonation.cdk();
            //CycleFinder cycles = Cycles.cdkAromaticSet();
            //Aromaticity aromaticity = new Aromaticity(model, cycles);





            //Homogenize pseudo atoms - all pseudo atoms (PA) as a "*"
            for (int u = 1; u < molecule.getAtomCount(); u++) {
                if (molecule.getAtom(u) instanceof IPseudoAtom) {

                    molecule.getAtom(u).setSymbol("*");
                    molecule.getAtom(u).setAtomTypeName("X");
                    ((IPseudoAtom) molecule.getAtom(u)).setLabel("*");

                }
            }




            try {
                String smi = sg.create(molecule);
                molecule = sp.parseSmiles(smi);
                molecule.setProperties(properties);
                molecule.setID(id);
            } catch (CDKException | IllegalArgumentException e) {
                e.printStackTrace();
            }


            // Addition of implicit hydrogens & atom typer
            CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(molecule.getBuilder());
            for (int j = 0; j < molecule.getAtomCount(); j++) {
                IAtom atom = molecule.getAtom(j);
                IAtomType type = null;
                try {
                    type = matcher.findMatchingAtomType(molecule, atom);
                    AtomTypeManipulator.configure(atom, type);
                } catch (CDKException e) {
                    e.printStackTrace();
                }

            }


            CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(molecule.getBuilder() );

            try {
                adder.addImplicitHydrogens(molecule);
            } catch (CDKException e) {
                e.printStackTrace();
            }

            AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
            AtomContainerManipulator.removeNonChiralHydrogens(molecule);




            try {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
                AtomContainerManipulator.percieveAtomTypesAndConfigureUnsetProperties(molecule);
                //Adding aromaticity to molecules when needed
                //aromaticity.apply(molecule);

            } catch (CDKException e) {
                e.printStackTrace();
            }




            //Fixing molecular bonds
            try {
                Kekulization.kekulize(molecule);

            } catch (CDKException e1) {
                //e1.printStackTrace();
            } catch (IllegalArgumentException e) {
                //System.out.println("Could not kekulize molecule "+ this.molecule.getID());
            }





            return molecule;
        }
        return null;
    }



    private boolean containsStrangeElements(IAtomContainer molecule) {
        if(molecule.getAtomCount()>0) {
            for (IAtom atom : molecule.atoms()) {
                if (!symbols2Check.contains(atom.getSymbol())) {
                    System.out.println("contains strange");
                    System.out.println(atom.getSymbol());
                    System.out.println(molecule.getID());
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isForbiddenMolecule(IAtomContainer molecule){
        String inchikey = molecule.getProperty("INCHIKEY");
        if(inchis2Check.contains(inchikey)){
            return true;
        }
        return false;

    }


}
