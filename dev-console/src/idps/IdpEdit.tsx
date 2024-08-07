import {
    BooleanField,
    BooleanInput,
    Datagrid,
    Edit,
    RecordContextProvider,
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
import { Page } from '../components/page';
import { PageTitle } from '../components/pageTitle';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import {
    getUiSchema,
    schemaOAuthClient,
    uiSchemaOAuthClient,
} from '../common/schemas';
import { Box } from '@mui/material';
import { TabToolbar } from '../components/TabToolbar';
import { SectionTitle } from '../components/sectionTitle';
import { RichTextInput } from 'ra-input-rich-text';
import { Editor } from '../components/AceEditorInput';
import { useForm, FormProvider, useFormContext } from 'react-hook-form';
import { attributeMappingFunction, authorizeFunction } from './hookFuntions';
import { CheckBoxInput } from '../components/CheckBoxInput';

const transform = async data => {
    console.log('data', data);
    return {
        ...data,

        settings: {
            hookFunctions: {
                ...data.settings?.hookFunctions,
                attributeMapping: data.attributeMapping?.base64,
                authorize: data.authorize?.base64,
            },
        },
    };
};
export const IdpEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="pessimistic"
                component={Box}
                transform={transform}
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
                        uiSchema={getUiSchema(record?.schema?.id)} //todo schema per tipo di ipd
                    />
                </TabbedForm.Tab>
                <TabbedForm.Tab label={translate('page.idp.hooks.title')}>
                    <RecordContextProvider
                        value={record?.settings.hookFunctions}
                    >
                        <SectionTitle
                            text={translate('page.idp.hooks.attribute')}
                            secondaryText={translate(
                                'page.idp.hooks.attributeDesc'
                            )}
                        />
                        <Box>
                            <CheckBoxInput
                                source="attributeMapping"
                                defaultFunction={attributeMappingFunction}
                            />
                            <Editor
                                source="attributeMapping"
                                mode="typescript"
                                theme="github"
                                defaultFunction={attributeMappingFunction}

                            ></Editor>
                        </Box>
                        <SectionTitle
                            text={translate('page.idp.hooks.authFunction')}
                            secondaryText={translate(
                                'page.idp.hooks.authFunctionDesc'
                            )}
                        />
                        <Box>
                            <CheckBoxInput
                                source="authorize"
                                defaultFunction={authorizeFunction}
                            />
                            <Editor
                                source="authorize"
                                mode="typescript"
                                theme="github"
                                defaultFunction={authorizeFunction}

                            ></Editor>
                        </Box>
                    </RecordContextProvider>
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
