import {
    Button,
    Datagrid,
    Edit,
    IconButtonWithTooltip,
    Labeled,
    ReferenceManyField,
    SaveButton,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    TranslatableInputs,
    useDataProvider,
    useDelete,
    useNotify,
    useRecordContext,
    useRefresh,
    useTranslate,
    useUpdate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { Alert, Box, Typography } from '@mui/material';
import { SectionTitle } from '../components/SectionTitle';
import { AceEditorInput } from '@dslab/ra-ace-editor';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { useRootSelector } from '@dslab/ra-root-selector';
import {
    getIdpSchema,
    getIdpUiSchema,
    schemaIdpSettings,
    uiSchemaIdpSettings,
} from './schemas';
import { ResourceTitle } from '../components/ResourceTitle';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import { getIdpIcon } from './utils';
import { IdField } from '../components/IdField';
import { useEffect, useMemo, useState } from 'react';
import { DEFAULT_LANGUAGES } from '../App';
import WarningIcon from '@mui/icons-material/WarningOutlined';
import RegisteredIcon from '@mui/icons-material/VerifiedUser';

export const IdpEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
            >
                <IdpEditTitle />
                <IdpEditForm />
            </Edit>
        </Page>
    );
};
const IdpEditTitle = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <ResourceTitle
            text={<IdpTitle />}
            icon={getIdpIcon(record.authority, {
                fontSize: 'large',
                sx: { fontSize: '96px' },
                color: 'primary',
            })}
        />
    );
};

const IdpTitle = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <Typography variant="h4" sx={{ pt: 0, pb: 1, textAlign: 'left' }}>
            {record.name}{' '}
            {record?.enabled && !record.registered && (
                <IconButtonWithTooltip
                    label={'error.idp_registration_error'}
                    color="error"
                >
                    <WarningIcon fontSize="small" />
                </IconButtonWithTooltip>
            )}
            {record.registered && (
                <IconButtonWithTooltip
                    label={'notification.idp_registered'}
                    color="success"
                >
                    <RegisteredIcon fontSize="small" />
                </IconButtonWithTooltip>
            )}
        </Typography>
    );
};

const IdpEditForm = () => {
    const translate = useTranslate();
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const record = useRecordContext();
    const schema = useMemo(() => getIdpSchema(record.schema), [record]);
    const [availableLocales, setAvailableLocales] =
        useState<string[]>(DEFAULT_LANGUAGES);

    useEffect(() => {
        if (dataProvider && realmId) {
            dataProvider.getOne('myrealms', { id: realmId }).then(data => {
                if (data && 'localizationConfiguration' in data) {
                    setAvailableLocales(
                        (data.localizationConfiguration as any)['languages'] ||
                            DEFAULT_LANGUAGES
                    );
                }
            });
        }
    }, [dataProvider, realmId]);

    if (!record) return null;

    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="id" />
                </Labeled>
                <Labeled>
                    <TextField source="name" />
                </Labeled>
                <Labeled>
                    <TextField source="type" />
                </Labeled>
                <Labeled>
                    <TextField source="authority" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text="page.idps.settings.basic.title"
                    secondaryText="page.idps.settings.basic.subtitle"
                />

                <TextInput source="name" fullWidth />

                <SectionTitle
                    text="page.idps.settings.display.title"
                    secondaryText="page.idps.settings.display.subtitle"
                />
                {availableLocales && (
                    <TranslatableInputs locales={availableLocales} fullWidth>
                        <TextInput source="titleMap" label="field.title.name" />
                        <TextInput
                            source="descriptionMap"
                            label="field.description.name"
                            multiline
                        />
                    </TranslatableInputs>
                )}

                <SectionTitle
                    text="page.idps.settings.advanced.title"
                    secondaryText="page.idps.settings.advanced.subtitle"
                />
                <JsonSchemaInput
                    source="settings"
                    schema={schemaIdpSettings}
                    uiSchema={uiSchemaIdpSettings}
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.configuration">
                <SectionTitle
                    text="page.idps.configuration.title"
                    secondaryText="page.idps.configuration.subtitle"
                />

                {schema && (
                    <JsonSchemaInput
                        source="configuration"
                        schema={schema}
                        uiSchema={getIdpUiSchema(record.schema)}
                    />
                )}
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.hooks">
                <SectionTitle
                    text="page.idps.hooks.attribute"
                    secondaryText="page.idps.hooks.attributeDesc"
                />
                <Box>
                    <AceEditorInput
                        source="settings.hookFunctions.attributeMapping"
                        mode="yaml"
                        theme="github"
                    ></AceEditorInput>
                </Box>
                <SectionTitle
                    text="page.idps.hooks.authFunction"
                    secondaryText="page.idps.hooks.authFunctionDesc"
                />
                <Box>
                    <AceEditorInput
                        source="settings.hookFunctions.authorize"
                        mode="yaml"
                        theme="github"
                    ></AceEditorInput>
                </Box>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.apps">
                <SectionTitle
                    text="page.idps.apps.title"
                    secondaryText="page.idps.apps.description"
                />

                <ReferenceManyField
                    reference="apps"
                    sort={{ field: 'name', order: 'ASC' }}
                    target="providers"
                    label="app"
                >
                    {/* {record.apps && record.apps.map(app =>  (
                            <TextField source="name" />
                        ))} */}

                    <Datagrid bulkActionButtons={false} sx={{ width: '100%' }}>
                        <TextField source="name" />
                        <IdField source="clientId" label="id" />
                    </Datagrid>
                </ReferenceManyField>
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const TabToolbar = () => {
    const record = useRecordContext();
    const translate = useTranslate();

    return (
        <>
            {record.enabled && (
                <Alert severity="warning" icon={<WarningIcon />} sx={{ mb: 2 }}>
                    {translate('error.idp_is_enabled')}
                </Alert>
            )}
            <Toolbar>
                <SaveButton />
            </Toolbar>
        </>
    );
};

const EditToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <ToggleIdpButton />
            <InspectButton />
            <DeleteWithDialogButton disabled={record?.registered} />
            <RefreshingExportButton />
        </TopToolbar>
    );
};

export const ToggleIdpButton = () => {
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();
    const notify = useNotify();
    const refresh = useRefresh();
    const [disable] = useDelete(
        'idps',
        {
            id: record.provider + '/status',
            meta: { realmId: realmId },
        },
        {
            onSuccess: () => {
                notify(record.id + ` disabled successfully`, {
                    type: 'warning',
                });
                refresh();
            },
            onError: error => {
                const msg =
                    error && typeof error === 'object' && 'message' in error
                        ? (error['message'] as string)
                        : 'ra.error.http_error';

                notify(msg, {
                    type: 'error',
                });
            },
        }
    );
    const [enable] = useUpdate(
        'idps',
        {
            id: record.provider + '/status',
            data: record,
            meta: { realmId: realmId },
        },
        {
            onSuccess: () => {
                notify(record.id + ` enabled successfully`, {
                    type: 'success',
                });
                refresh();
            },
            onError: error => {
                const msg =
                    error && typeof error === 'object' && 'message' in error
                        ? (error['message'] as string)
                        : 'ra.error.http_error';

                notify(msg, {
                    type: 'error',
                });
            },
        }
    );

    if (!record) return null;
    return (
        <>
            {record.enabled && (
                <Button
                    onClick={e => {
                        disable();
                        e.stopPropagation();
                    }}
                    label="Disable"
                    color="warning"
                    startIcon={<StopIcon />}
                ></Button>
            )}
            {!record.enabled && (
                <Button
                    onClick={e => {
                        enable();
                        e.stopPropagation();
                    }}
                    label="Enable"
                    color="success"
                    startIcon={<PlayArrowIcon />}
                ></Button>
            )}
        </>
    );
};
