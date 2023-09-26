import { Typography } from '@mui/material';
import {
    BooleanField,
    Datagrid,
    DateField,
    EditButton,
    NumberField,
    ReferenceManyField,
    RichTextField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useNotify,
    useRecordContext,
    useRedirect,
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
            <Typography variant="h5" sx={{ mr: 2, mt: 1 }}>
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" sx={{ mr: 2 }}>
                {record.id}
            </Typography>
            <br />
            <TabbedShowLayout sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Overview">
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="clientId" />
                    <TextField source="scopes" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Quickstart">
                    <RichTextField source="body" label={false} />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Credentials">
                    <TextField
                        label="Password (if protected post)"
                        source="password"
                        type="password"
                    />
                    <DateField label="Publication date" source="published_at" />
                    <NumberField source="average_note" />
                    <BooleanField
                        label="Allow comments?"
                        source="commentable"
                    />
                    <TextField label="Nb views" source="views" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Test">
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
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Endpoint">
                    <RichTextField source="body" label={false} />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const ShowToolBarActions = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to = `/apps/r/${realmId}/${record.id}/edit`;
    if (!record) return null;
    return (
        <TopToolbar>
            <>
                <EditButton to={to}></EditButton>
            </>
        </TopToolbar>
    );
};
