import { Box, Divider } from '@mui/material';
import {
    Edit,
    Labeled,
    ReferenceArrayInput,
    ShowButton,
    TabbedForm,
    TextField,
    TextInput,
    TopToolbar,
    useEditContext,
    useGetList,
    useNotify,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { InspectButton } from '@dslab/ra-inspect-button';
import { PageTitle } from '../components/pageTitle';
import { SectionTitle } from '../components/sectionTitle';
import { schemaOAuthClient, uiSchemaOAuthClient } from '../common/schemas';
import { Page } from '../components/page';
import { TabToolbar } from '../components/TabToolbar';
import { AppIcon } from './AppIcon';
import { AppTitle } from './AppTitle';

export const AppEdit = () => {
    //fetch related to resolve relations
    const { data: roles } = useGetList('roles', {
        pagination: { page: 1, perPage: 100 },
        sort: { field: 'roleId', order: 'ASC' },
    });
    const { data: groups } = useGetList('groups', {
        pagination: { page: 1, perPage: 100 },
        sort: { field: 'groupId', order: 'ASC' },
    });

    //inflate back flattened fields
    const transform = data => ({
        ...data,
        roles: roles?.filter(r => data.roles.includes(r.id)),
        groups: groups?.filter(r => data.groups.includes(r.id)),
    });

    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
                transform={transform}
                queryOptions={{ meta: { flatten: ['roles', 'groups'] } }}
            >
                <AppTitle />
                <AppEditForm />
            </Edit>
        </Page>
    );
};

const AppEditForm = () => {
    const translate = useTranslate();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    if (!record) return null;
    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label={translate('page.app.overview.title')}>
                <Labeled>
                    <TextField source="name" />
                </Labeled>
                <Labeled>
                    <TextField source="type" />
                </Labeled>
                <Labeled>
                    <TextField source="clientId" />
                </Labeled>
                <Labeled>
                    <TextField source="scopes" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label={translate('page.app.settings.title')}>
                <SectionTitle
                    text={translate('page.app.settings.header.title')}
                    secondaryText={translate(
                        'page.app.settings.header.subtitle'
                    )}
                />
                <TextInput source="name" fullWidth />
                <TextInput source="description" fullWidth />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="Providers">
                <SectionTitle
                    text={translate('page.app.providers.header.title')}
                    secondaryText={translate(
                        'page.app.providers.header.subtitle'
                    )}
                />
                <ReferenceArrayInput source="providers" reference="idps" />
            </TabbedForm.Tab>

            <TabbedForm.Tab label={translate('page.app.configuration.title')}>
                <SectionTitle
                    text={translate('page.app.configuration.header.title')}
                    secondaryText={translate(
                        'page.app.configuration.header.subtitle'
                    )}
                />
                <JsonSchemaInput
                    source="configuration"
                    schema={schemaOAuthClient}
                    uiSchema={uiSchemaOAuthClient}
                />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="Api access">
                <SectionTitle
                    text={translate('page.app.scopes.header.title')}
                    secondaryText={translate('page.app.scopes.header.subtitle')}
                />
                <ReferenceArrayInput source="scopes" reference="scopes" />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="Roles">
                <SectionTitle
                    text={translate('page.app.roles.header.title')}
                    secondaryText={translate('page.app.roles.header.subtitle')}
                />
                <ReferenceArrayInput source="roles" reference="roles" />
            </TabbedForm.Tab>

            <TabbedForm.Tab label="Groups">
                <SectionTitle
                    text={translate('page.app.groups.header.title')}
                    secondaryText={translate('page.app.groups.header.subtitle')}
                />
                <ReferenceArrayInput source="groups" reference="groups" />
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const EditSetting = () => {
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    return (
        <Box>
            <Box display="flex">
                <Box flex="1" mt={-1}>
                    <Box display="flex" width={430}>
                        <TextInput source="name" fullWidth />
                    </Box>
                    <Box display="flex" width={430}></Box>
                    <Divider />
                </Box>
            </Box>
        </Box>
    );
};

const EditToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <ShowButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton language="yaml" color="info" />
        </TopToolbar>
    );
};
