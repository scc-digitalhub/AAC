import { Edit, SaveButton, ShowButton, TabbedForm, TextField, Toolbar, TopToolbar, useRecordContext, useTranslate } from 'react-admin';
import { useParams } from 'react-router-dom';
import React from 'react';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { EnableIdpButton } from './IdpList';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/page';
import { PageTitle } from '../components/pageTitle';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { schemaOAuthClient, uiSchemaOAuthClient } from '../common/schemas';
import { Box } from '@mui/material';

export const IdpEdit = () => {
    return (
        <Page>
            <Edit actions={<EditToolBarActions />} mutationMode="pessimistic" component={Box}>
                <IdpTabComponent />
            </Edit>
        </Page>
    );
};
const MyToolbar = () => (
    <Toolbar>
        <SaveButton />
    </Toolbar>
);
const IdpTabComponent = () => {
    const record = useRecordContext();
    const translate = useTranslate();
    if (!record) return null;
    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />
            <TabbedForm toolbar={<MyToolbar />}>
                <TabbedForm.Tab  label={translate('page.idp.overview.title')}>
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="authority" />
                    <TextField source="provider" />
                    <TextField source="enabled" />
                    <TextField source="registered" />
                </TabbedForm.Tab>
                <TabbedForm.Tab
                    label={translate('page.idp.settings.title')}
                >
                    <OAuthJsonSchemaForm />
                </TabbedForm.Tab>
                <TabbedForm.Tab
                    label={translate('page.idp.configuration.title')}
                >
                   
                    <OAuthJsonSchemaForm />
                </TabbedForm.Tab>
                <TabbedForm.Tab
                    label={translate('page.idp.hooks.title')}
                >
                    
                </TabbedForm.Tab>
                <TabbedForm.Tab
                    label={translate('page.idp.app.title')}
                >
                    
                </TabbedForm.Tab>
            </TabbedForm>
        </>
    );
};
const OAuthJsonSchemaForm = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <JsonSchemaInput
            source="configuration"
            schema={schemaOAuthClient}
            uiSchema={uiSchemaOAuthClient}
        />
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
