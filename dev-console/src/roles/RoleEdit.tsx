import { Box } from '@mui/material';
import {
    Edit,
    Labeled,
    NumberField,
    ReferenceArrayInput,
    TabbedForm,
    TextField,
    TextInput,
    TopToolbar,
    useGetOne,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { PageTitle } from '../components/PageTitle';
import { TabToolbar } from '../components/TabToolbar';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';
import { SectionTitle } from '../components/sectionTitle';
import { DatagridArrayInput } from '@dslab/ra-datagrid-input';
import { IdField } from '../components/IdField';

export const RoleEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
            >
                <ResourceTitle />
                <RoleEditForm />
            </Edit>
        </Page>
    );
};

const RoleEditForm = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="Overview">
                <Labeled>
                    <TextField source="id" />
                </Labeled>
                <Labeled>
                    <TextField source="role" />
                </Labeled>
                <Labeled>
                    <TextField source="authority" />
                </Labeled>
                <Labeled>
                    <NumberField source="size" label="subjects" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="Settings">
                <SectionTitle
                    text={translate('page.role.settings.header.title')}
                    secondaryText={translate(
                        'page.group.settings.header.subtitle'
                    )}
                />
                <TextInput source="role" fullWidth readOnly />
                <TextInput source="name" fullWidth />
                <TextInput source="description" multiline fullWidth />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="Permission">
                <SectionTitle
                    text={translate('page.roles.permissions.header.title')}
                    secondaryText={translate(
                        'page.roles.permissions.header.subtitle'
                    )}
                />
                <ReferenceArrayInput source="permissions" reference="scopes" />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="Subjects">
                <SectionTitle
                    text={translate('page.roles.subjects.header.title')}
                    secondaryText={translate(
                        'page.group.subjects.members.subtitle'
                    )}
                />
                <ReferenceArrayInput source="subjects" reference="subjects">
                    <DatagridArrayInput
                        dialogFilters={[<TextInput label="query" source="q" />]}
                        dialogFilterDefaultValues={{ q: '' }}
                    >
                        <TextField source="name" />
                        <TextField source="type" />
                        <IdField source="id" />
                    </DatagridArrayInput>
                </ReferenceArrayInput>
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const EditToolBarActions = () => {
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
