import { Typography } from '@mui/material';
import {
    EditButton,
    RichTextField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { PageTitle } from '../components/pageTitle';
import { TabTitle } from '../components/tabTitle';

export const AppShow = () => {
    return (
        <Show actions={<ShowToolBarActions />}>
            <AppTabComponent />
        </Show>
    );
};

const AppTabComponent = () => {
    const record = useRecordContext();
    const translate = useTranslate();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />
            <TabbedShowLayout syncWithLocation={false}>
                <TabbedShowLayout.Tab
                    label={translate('page.app.overview.title')}
                >
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="clientId" />
                    <TextField source="scopes" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.app.credentials.title')}
                >
                    <TabTitle
                        text={translate('page.app.credentials.header.title')}
                        secondaryText={translate(
                            'page.app.credentials.header.subtitle'
                        )}
                    />
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
