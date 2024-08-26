import { Box } from '@mui/material';
import {
    AutocompleteArrayInput,
    Edit,
    Labeled,
    NumberField,
    ReferenceArrayInput,
    SaveButton,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useGetList,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { IdField } from '../components/IdField';
import { Page } from '../components/Page';
import { DatagridArrayInput } from '@dslab/ra-datagrid-input';
import { SectionTitle } from '../components/SectionTitle';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';

export const GroupEdit = () => {
    //fetch related to resolve relations
    const { data: roles } = useGetList('roles', {
        pagination: { page: 1, perPage: 100 },
        sort: { field: 'name', order: 'ASC' },
    });

    //inflate back flattened fields
    const transform = data => ({
        ...data,
        roles: roles?.filter(r => data.roles.includes(r.id)),
    });

    return (
        <Page>
            <Edit
                actions={<ActionsToolbar />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
                transform={transform}
                queryOptions={{ meta: { flatten: ['roles'] } }}
                mutationOptions={{ meta: { flatten: ['roles'] } }}
            >
                <ResourceTitle />
                <GroupEditForm />
            </Edit>
        </Page>
    );
};

const GroupEditForm = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="id" label="field.id.name" />
                </Labeled>
                <Labeled>
                    <TextField source="name" label="field.name.name" />
                </Labeled>
                <Labeled>
                    <TextField source="group" label="field.group.name" />
                </Labeled>
                <Labeled>
                    <NumberField source="size" l label="field.members.name" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text={translate('page.group.settings.header.title')}
                    secondaryText={translate(
                        'page.group.settings.header.subtitle'
                    )}
                />
                <TextInput
                    source="group"
                    label="field.group.name"
                    helperText="field.group.helperText"
                    fullWidth
                    readOnly
                />
                <TextInput
                    source="name"
                    label="field.name.name"
                    helperText="field.name.helperText"
                    fullWidth
                />
                <TextInput
                    source="description"
                    label="field.description.name"
                    helperText="field.description.helperText"
                    multiline
                    fullWidth
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.roles">
                <SectionTitle
                    text={translate('page.group.roles.header.title')}
                    secondaryText={translate(
                        'page.group.roles.header.subtitle'
                    )}
                />
                <ReferenceArrayInput source="roles" reference="roles">
                    <AutocompleteArrayInput
                        label="field.roles.name"
                        helperText="field.roles.helperText"
                    />
                </ReferenceArrayInput>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.members">
                <SectionTitle
                    text={translate('page.group.members.header.title')}
                    secondaryText={translate(
                        'page.group.roles.members.subtitle'
                    )}
                />
                <ReferenceArrayInput source="members" reference="subjects">
                    <DatagridArrayInput
                        dialogFilters={[<TextInput label="query" source="q" />]}
                        dialogFilterDefaultValues={{ q: '' }}
                        label="field.members.name"
                        helperText="field.members.helperText"
                    >
                        <TextField source="name" label="field.name.name" />
                        <TextField source="type" label="field.type.name" />
                        <IdField source="id" label="field.id.name" />
                    </DatagridArrayInput>
                </ReferenceArrayInput>
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const ActionsToolbar = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <InspectButton />
            <DeleteWithDialogButton />
            <RefreshingExportButton />
        </TopToolbar>
    );
};
const TabToolbar = () => (
    <Toolbar>
        <SaveButton />
    </Toolbar>
);
