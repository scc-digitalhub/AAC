import {
    Button,
    CheckboxGroupInput,
    Datagrid,
    Edit,
    IconButtonWithTooltip,
    Labeled,
    ReferenceArrayInput,
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
import { TabToolbar } from '../components/TabToolbar';
import { SectionTitle } from '../components/sectionTitle';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { useRootSelector } from '@dslab/ra-root-selector';
import {
    getApSchema,
    getApUiSchema,
    schemaApSettings,
    uiSchemaApSettings,
} from './schemas';
import { ResourceTitle } from '../components/ResourceTitle';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import { getApIcon } from './utils';
import { IdField } from '../components/IdField';
import { useEffect, useMemo, useState } from 'react';
import dataProvider from '../dataProvider';
import { DEFAULT_LANGUAGES } from '../App';
import WarningIcon from '@mui/icons-material/WarningOutlined';
import RegisteredIcon from '@mui/icons-material/VerifiedUser';
import { PageTitle } from '../components/PageTitle';
import { IdpNameField } from '../idps/IdpList';
import { AttributeSetIcon } from '../attributeset/AttributeSetIcon';
import { NameField } from '../components/NameField';
import utils from '../utils';
import { AceEditorInput } from '../components/AceEditorInput';

export const ApEdit = () => {
    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
            >
                <ApEditTitle />
                <ApEditForm />
            </Edit>
        </Page>
    );
};
const ApEditTitle = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <ResourceTitle
            text={<ApTitle />}
            icon={getApIcon(record.authority, {
                fontSize: 'large',
                sx: { fontSize: '96px' },
                color: 'primary',
            })}
        />
    );
};

const ApTitle = () => {
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

const ApEditForm = () => {
    const translate = useTranslate();
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const record = useRecordContext();
    const schema = useMemo(() => getApSchema(record.schema), [record]);
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
        <TabbedForm toolbar={<EditTabToolbar />} syncWithLocation={false}>
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
                    text="page.aps.settings.basic.title"
                    secondaryText="page.aps.settings.basic.subtitle"
                />

                <TextInput source="name" fullWidth />

                <SectionTitle
                    text="page.aps.settings.display.title"
                    secondaryText="page.aps.settings.display.subtitle"
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
                    text="page.aps.settings.advanced.title"
                    secondaryText="page.aps.settings.advanced.subtitle"
                />
                <JsonSchemaInput
                    source="settings"
                    schema={schemaApSettings}
                    uiSchema={uiSchemaApSettings}
                />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.attributeSets">
                <SectionTitle
                    text="page.aps.attributeSets.header.title"
                    secondaryText="page.aps.attributeSets.header.subtitle"
                />
                {/* <ReferenceArrayInput
                    source="providers"
                    reference="aps"
                    sort={{ field: 'name', order: 'ASC' }}
                /> */}
                <ReferenceArrayInput
                    source="settings.attributeSets"
                    reference="attributeset"
                    sort={{ field: 'name', order: 'ASC' }}
                >
                    <CheckboxGroupInput
                        row={false}
                        labelPlacement="end"
                        optionText={
                            <NameField
                                text="name"
                                secondaryText="identifier"
                                source="name"
                                icon={<AttributeSetIcon color={'secondary'} />}
                            />
                        }
                        sx={{ textAlign: 'left' }}
                    />
                </ReferenceArrayInput>
            </TabbedForm.Tab>

            <TabbedForm.Tab label="tab.configuration">
                <SectionTitle
                    text="page.aps.configuration.title"
                    secondaryText="page.aps.configuration.subtitle"
                />

                {schema && (
                    <JsonSchemaInput
                        source="configuration"
                        schema={getApSchema(record.schema)}
                        uiSchema={getApUiSchema(record.schema)}
                    />
                )}

                {schema &&
                    schema['$id'] ===
                        'urn:jsonschema:it:smartcommunitylab:aac:attributes:provider:ScriptAttributeProviderConfigMap' && (
                        <AceEditorInput
                            source="configuration.code"
                            mode="javascript"
                            isRequired={true}
                            parse={utils.parseBase64}
                            format={utils.encodeBase64}
                            minLines={25}
                        />
                    )}
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

export const EditTabToolbar = () => {
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
    const translate = useTranslate();
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <ToggleApButton />
            <InspectButton />
            <DeleteWithDialogButton disabled={record?.registered} />
            <RefreshingExportButton />
        </TopToolbar>
    );
};

export const ToggleApButton = () => {
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();
    const notify = useNotify();
    const refresh = useRefresh();
    const [disable] = useDelete(
        'aps',
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
        'aps',
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
