import { Typography } from '@mui/material';
import {
    EditButton,
    RichTextField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';

export const AppShow = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    return (
        <Show queryOptions={options} actions={<ShowToolBarActions />}>
            <AppTabComponent />
        </Show>
    );
};

const AppTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <br />
            <Typography variant="h5" >
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" >
                {record.id}
            </Typography>
            <br />
            <TabbedShowLayout  syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Overview">
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="clientId" />
                    <TextField source="scopes" />
                </TabbedShowLayout.Tab>
                {/* <TabbedShowLayout.Tab label="Quickstart">
                    <RichTextField source="body" label={false} />
                </TabbedShowLayout.Tab> */}
                <TabbedShowLayout.Tab label="Credentials">
                    <Typography variant="h6" >
                        OAuth2 Credentials
                    </Typography>
                    <Typography >
                        Client credentials to authenticate with AAC.
                    </Typography>
                    <TextField label="Client ID" source="clientId" />
                    {record.configuration.clientSecret && (
                        <TextField
                            label="Client Key Set (JWKS)"
                            source="configuration.clientSecret"
                        />
                    )}
                    {record.configuration.jwks && (
                        <RichTextField
                            label="Client Secret"
                            source="configuration.jwks"
                        />
                    )}
                </TabbedShowLayout.Tab>
                {/* <TabbedShowLayout.Tab label="Test">
                    <ReferenceManyField
                        reference="comments"
                        target="post_id"
                        label={false}
                    >
                        <Datagrid>
                            <TextField source="body" />
                            <DateField source="created_at" />
                            <EditButton />
                        </Datagrid>
                    </ReferenceManyField>
                </TabbedShowLayout.Tab> */}
                {/* <TabbedShowLayout.Tab label="Endpoint">
                    <RichTextField source="body" label={false} />
                </TabbedShowLayout.Tab> */}
            </TabbedShowLayout>
        </>
    );
};

const ShowToolBarActions = () => {
    return (
        <TopToolbar>
            <>
                <EditButton></EditButton>
            </>
        </TopToolbar>
    );
};
