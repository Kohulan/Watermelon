import Row from "react-bootstrap/Row";
import Col from "react-bootstrap/Col";
import Container from "react-bootstrap/Container";
import Spinner from "../Spinner";
import Error from "../Error";
import Utils from "../../Utils";
import NavigationSidebar from "./NavigationSidebar";
import Overview from "./Overview";
import Representations from "./Representations";
import MolecularProperties from "./MolecularProperties";
import MolecularDescriptors from "./MolecularDescriptors";
import CrossReferences from "./CrossReferences";
import KnownStereochemicalVariants from "./KnownStereochemicalVariants";
import References from "./References";
import Synonyms from "./Synonyms";
import ChemClassification from "./ChemClassification";
import PredictedActivity from "./PredictedActivity";
import Organisms from "./Organisms";
import Geography from "./Geography";

const React = require("react");
const restClient = require("../../restClient");


export default class NaturalProductCompoundCard extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            error: null,
            isLoaded: false,
            naturalProduct: []
        };
    }

    componentDidMount() {
        const identifier = this.props.match.params.identifier;
        const identifierValue = this.props.match.params.identifierValue;

        this.fetchNaturalProductByIdentifier(identifier, identifierValue);
    }

    fetchNaturalProductByIdentifier(identifier, identifierValue) {
        restClient({
            method: "GET",
            path: "/api/compound/search/findByAfc_id" + "?" + identifier + "=" + encodeURIComponent(identifierValue)
        }).then(
            (response) => {
                this.setState({
                    isLoaded: true,
                    naturalProduct: response.entity._embedded.watermelonMolecules[0]
                });
            },
            (error) => {
                this.setState({
                    isLoaded: true,
                    error: error
                });
            });
    }

    render() {
        const { error, isLoaded, naturalProduct } = this.state;

        if (error) {
            return <Error/>;
        } else if (!isLoaded) {
            return <Spinner/>;
        } else {
            const compoundCardItems = [
                "overview",
                "representations",
                "synonyms", //TODO add concentrations and plant part
                "molecular_properties",
                "molecular_descriptors",
                "cross_references"



            ];


            return (
                <Container>
                    <Row>

                        <Col sm={12}>
                            <Row id={compoundCardItems[0]} className="compoundCardRow">
                                <Overview naturalProduct={naturalProduct}/>
                            </Row>
                            <br/>
                            <Row id={compoundCardItems[1]} className="compoundCardRow">
                                <Representations naturalProduct={naturalProduct}/>
                            </Row>
                            <br/>
                            
                            <Row id={compoundCardItems[3]} className="compoundCardRow">
                                <MolecularProperties naturalProduct={naturalProduct}/>
                            </Row>

                            <br/>
                            <Row id={compoundCardItems[5]} className="compoundCardRow">
                                <MolecularDescriptors naturalProduct={naturalProduct}/>
                            </Row>
                            <br/>

                            <Row id={compoundCardItems[6]} className="compoundCardRow">
                                <CrossReferences naturalProduct={naturalProduct}/>
                            </Row>
                            <br/>
                            

                            <Row id={compoundCardItems[2]} className="compoundCardRow">
                                <Synonyms naturalProduct={naturalProduct}/>
                            </Row>
                            <br/>


                        </Col>
                    </Row>
                </Container>
            );
        }
    }
}