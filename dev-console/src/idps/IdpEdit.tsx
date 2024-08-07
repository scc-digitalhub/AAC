import {
    BooleanField,
    BooleanInput,
    Datagrid,
    Edit,
    ReferenceManyField,
    SaveButton,
    SelectInput,
    ShowButton,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    TranslatableInputs,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import React from 'react';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { EnableIdpButton } from './IdpList';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { PageTitle } from '../components/PageTitle';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { getUiSchema } from '../common/schemas';
import { Box } from '@mui/material';
import { TabToolbar } from '../components/TabToolbar';
import { SectionTitle } from '../components/sectionTitle';
import { RichTextInput } from 'ra-input-rich-text';
import { AceEditorInput } from '@dslab/ra-ace-editor';

export const IdpEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="pessimistic"
                component={Box}
            >
                <IdpTabComponent />
            </Edit>
        </Page>
    );
};

const IdpTabComponent = () => {
    const record = useRecordContext();
    const translate = useTranslate();
    if (!record) return null;
    return (
        <>
            <PageTitle
                text={record.name}
                secondaryText={record?.id}
                copy={true}
            />
            <TabbedForm toolbar={<TabToolbar />}>
                <TabbedForm.Tab label={translate('page.idp.overview.title')}>
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="authority" />
                    <TextField source="provider" />
                    <TextField source="enabled" />
                    <TextField source="registered" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.idp.settings.title')}>
                    <SectionTitle text={translate('page.idp.settings.basic')} />
                    <TextInput source="name" fullWidth />
                    <SectionTitle
                        text={translate('page.idp.settings.display')}
                    />
                    <TranslatableInputs locales={['it', 'en', 'de']} fullWidth>
                        <TextInput source="titleMap" />
                        <TextInput source="descriptionMap" multiline />
                    </TranslatableInputs>
                    <SectionTitle
                        text={translate('page.idp.settings.advanced')}
                    />

                    <SelectInput
                        fullWidth
                        source="settings.events"
                        choices={settingEvents}
                        label="Events"
                    />
                    <SelectInput
                        fullWidth
                        source="settings.persistence"
                        choices={settingPersistences}
                        label="Persistence"
                    />
                    <BooleanInput source="settings.linkable" />
                </TabbedForm.Tab>
                <TabbedForm.Tab
                    label={translate('page.idp.configuration.title')}
                >
                    <JsonSchemaInput
                        source="configuration"
                        schema={record.schema}
                        uiSchema={getUiSchema(record?.schema?.id)}
                    />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.idp.hooks.title')}>
                    <SectionTitle
                        text={translate('page.idp.hooks.attribute')}
                        secondaryText={translate(
                            'page.idp.hooks.attributeDesc'
                        )}
                    />
                    <Box>
                        <AceEditorInput
                            source="settings.hookFunctions.attributeMapping"
                            mode="yaml"
                            theme="github"
                        ></AceEditorInput>
                    </Box>
                    <SectionTitle
                        text={translate('page.idp.hooks.authFunction')}
                        secondaryText={translate(
                            'page.idp.hooks.authFunctionDesc'
                        )}
                    />
                    <Box>
                        <AceEditorInput
                            source="settings.hookFunctions.authorize"
                            mode="yaml"
                            theme="github"
                        ></AceEditorInput>
                    </Box>
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.idp.app.title')}>
                    <ReferenceManyField
                        reference="apps"
                        target="providers"
                        label="app"
                    >
                        {/* {record.apps && record.apps.map(app =>  (
                            <TextField source="name" />
                        ))} */}

                        <Datagrid bulkActionButtons={false}>
                            <TextField source="id" />
                            <TextField source="name" />
                            {/* <BooleanInput label="Enable" source="idps" /> */}
                        </Datagrid>
                    </ReferenceManyField>
                </TabbedForm.Tab>
            </TabbedForm>
        </>
    );
};

const EditToolBarActions = () => {
    const [open, setOpen] = React.useState(false);
    const record = useRecordContext();
    if (!record) return null;
    return (
        <TopToolbar>
            <EnableIdpButton />
            <ShowButton />

            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton />
        </TopToolbar>
    );
};
const settingEvents = [
    { id: 'none', name: 'none' },
    { id: 'minimal', name: 'minimal' },
    { id: 'details', name: 'details' },
    { id: 'full', name: 'full' },
];
const settingPersistences = [
    { id: 'none', name: 'none' },
    { id: 'session', name: 'session' },
    { id: 'memory', name: 'memory' },
    { id: 'repository', name: 'repository' },
];
