import { Box } from '@mui/material';
import {
    AutocompleteArrayInput,
    Edit,
    Labeled,
    NumberField,
    ReferenceArrayInput,
    SaveButton,
    SelectInput,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    ToolbarClasses,
    TopToolbar,
    useDataProvider,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';
import { SectionTitle } from '../components/SectionTitle';
import { DatagridArrayInput } from '@dslab/ra-datagrid-input';
import { IdField } from '../components/IdField';
import { useEffect, useState } from 'react';
import dataProvider from '../dataProvider';
import { useRootSelector } from '@dslab/ra-root-selector';
import { DEFAULT_LANGUAGES } from '../App';
import { AceEditorInput } from '@dslab/ra-ace-editor';
import { TemplatesEditorInput } from './TemplatesEditor';
import { TemplatesPreviewButton } from './TemplatesPreview';

export const TemplateEdit = () => {
    return (
        <Page>
            <Edit
                actions={<ActionsToolbar />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
            >
                <ResourceTitle />
                <TemplateEditForm />
            </Edit>
        </Page>
    );
};

const TemplateEditForm = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const [template, setTemplate] = useState<any>();
    const [availableLocales, setAvailableLocales] =
        useState<string[]>(DEFAULT_LANGUAGES);

    useEffect(() => {
        if (dataProvider && realmId && record) {
            dataProvider.getOne('myrealms', { id: realmId }).then(data => {
                if (data && 'localizationConfiguration' in data) {
                    setAvailableLocales(
                        (data.localizationConfiguration as any)['languages'] ||
                            DEFAULT_LANGUAGES
                    );
                }
            });
            dataProvider
                .invoke({
                    path:
                        'templates/' +
                        realmId +
                        '/' +
                        record.authority +
                        '/' +
                        record.template,
                })
                .then(data => {
                    setTemplate({ ...data, size: data.keys.length });
                })
                .catch(error => {
                    setTemplate(undefined);
                });
        }
    }, [record, dataProvider, realmId]);

    if (!record) return null;

    const keys = template ? template.keys.sort() : [];

    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="id" label="field.id.name" />
                </Labeled>
                <Labeled>
                    <TextField source="template" label="field.template.name" />
                </Labeled>
                <Labeled>
                    <TextField
                        source="authority"
                        label="field.authority.name"
                    />
                </Labeled>
                <Labeled>
                    <NumberField
                        record={template}
                        source="size"
                        label="field.keys.name"
                    />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text="page.templates.settings.title"
                    secondaryText="page.templates.settings.subtitle"
                />
                <TextInput
                    source="template"
                    label="field.template.name"
                    helperText="field.template.helperText"
                    fullWidth
                    readOnly
                />
                <SelectInput
                    source="language"
                    required
                    label="field.language.name"
                    helperText="field.language.helperText"
                    choices={availableLocales.sort().map(a => ({
                        id: a,
                        name: a,
                    }))}
                />
            </TabbedForm.Tab>
            {keys.map(k => (
                <TabbedForm.Tab label={k}>
                    <SectionTitle
                        text="page.templates.key.title"
                        secondaryText="page.templates.key.subtitle"
                    />
                    <TemplatesEditorInput source="content" field={k} />
                </TabbedForm.Tab>
            ))}
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
            <RefreshingExportButton filename={`templates-${record.id}`} />
        </TopToolbar>
    );
};

const TabToolbar = () => (
    <Toolbar>
        <div className={ToolbarClasses.defaultToolbar}>
            <SaveButton />
            <TemplatesPreviewButton variant="contained" size="medium" />
        </div>
    </Toolbar>
);
