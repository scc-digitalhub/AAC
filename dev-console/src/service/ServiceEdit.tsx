import { Box, Typography } from '@mui/material';
import {
    ArrayField,
    BooleanField,
    Datagrid,
    DeleteWithConfirmButton,
    Edit,
    FunctionField,
    Labeled,
    SaveButton,
    ShowButton,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useDataProvider,
    useGetResourceLabel,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { PageTitle } from '../components/PageTitle';
import { TabToolbar } from '../components/TabToolbar';
import { ResourceTitle } from '../components/ResourceTitle';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { SectionTitle } from '../components/sectionTitle';
import { NameField } from '../components/NameField';
import {
    CreateInDialogButton,
    EditInDialogButton,
} from '@dslab/ra-dialog-crud';
import { ScopeEditForm } from './ServiceScope';
import { useRootSelector } from '@dslab/ra-root-selector';
import { RowButtonGroup } from '../components/RowButtonGroup';
import { ClaimEditForm } from './ServiceClaim';
import { ClaimMappingEditor } from '../components/ClaimMappingEditor';

const claimMappingDefaultValue =
    'LyoqCiAqIERFRklORSBZT1VSIE9XTiBDTEFJTSBNQVBQSU5HIEhFUkUKKiovCmZ1bmN0aW9uIGNsYWltTWFwcGluZyhjb250ZXh0KSB7CiBsZXQgY2xpZW50ID0gY29udGV4dC5jbGllbnQ7IAogbGV0IHVzZXIgPSBjb250ZXh0LnVzZXI7IAogbGV0IHNjb3BlcyA9IGNvbnRleHQuc2NvcGVzOyAKICByZXR1cm4ge307Cn0';

export const ServiceEdit = () => {
    const refresh = useRefresh();

    return (
        <Page>
            <Edit
                actions={<EditToolBarActions />}
                mutationMode="pessimistic"
                mutationOptions={{ onSettled: () => refresh() }}
                component={Box}
                redirect={'edit'}
            >
                <ResourceTitle />
                <ServiceEditForm />
            </Edit>
        </Page>
    );
};

const ServiceEditForm = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();
    const refresh = useRefresh();
    const getResourceLabel = useGetResourceLabel();
    const dataProvider = useDataProvider();

    if (!record) return null;

    const serviceId = record.id;
    const scopeLabel = getResourceLabel('scopes', 1);
    const claimLabel = getResourceLabel('claims', 1);

    const handleValidate = name => (record, code) => {
        return dataProvider.invoke({
            path: 'services/' + realmId + '/' + serviceId + '/claims/validate',
            body: JSON.stringify({
                code,
                name: name,
                scopes: [],
            }),
            options: {
                method: 'POST',
            },
        });
    };

    return (
        <TabbedForm toolbar={<ServiceTabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="id" />
                </Labeled>
                <Labeled>
                    <TextField source="name" />
                </Labeled>
                <Labeled>
                    <TextField source="namespace" />
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text={translate('page.service.settings.header.title')}
                    secondaryText={translate(
                        'page.service.settings.header.subtitle'
                    )}
                />
                <TextInput source="namespace" fullWidth readOnly />
                <TextInput source="name" fullWidth />
                <TextInput source="description" multiline fullWidth />
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.scopes">
                <SectionTitle
                    text={translate('page.service.scopes.header.title')}
                    secondaryText={translate(
                        'page.service.scopes.header.subtitle'
                    )}
                />
                <TopToolbar sx={{ width: '100%' }}>
                    <CreateInDialogButton
                        variant="contained"
                        title={translate('ra.page.create', {
                            name: scopeLabel,
                        })}
                        maxWidth={'md'}
                        fullWidth
                        resource={
                            'services/' + realmId + '/' + serviceId + '/scopes'
                        }
                        mutationOptions={{ onSettled: () => refresh() }}
                        record={{ serviceId }}
                    >
                        <ScopeEditForm mode="create" />
                    </CreateInDialogButton>
                </TopToolbar>
                <ArrayField source="scopes">
                    <Datagrid bulkActionButtons={false} sx={{ width: '100%' }}>
                        <NameField
                            text="name"
                            secondaryText="scope"
                            source="name"
                            icon={false}
                        />
                        <TextField source="type" />
                        <RowButtonGroup>
                            <EditInDialogButton
                                title={translate('ra.page.edit', {
                                    name: translate('ra.action.edit'),
                                    recordRepresentation: scopeLabel,
                                })}
                                maxWidth={'md'}
                                fullWidth
                                resource={
                                    'services/' +
                                    realmId +
                                    '/' +
                                    serviceId +
                                    '/scopes'
                                }
                                queryOptions={{ meta: { root: '' } }}
                                mutationMode="optimistic"
                                mutationOptions={{ onSettled: () => refresh() }}
                            >
                                <ScopeEditForm mode="edit" />
                            </EditInDialogButton>
                            <DeleteWithConfirmButton
                                resource={
                                    'services/' +
                                    realmId +
                                    '/' +
                                    serviceId +
                                    '/scopes'
                                }
                                redirect={false}
                                mutationMode="pessimistic"
                                mutationOptions={{ onSettled: () => refresh() }}
                            />
                        </RowButtonGroup>
                    </Datagrid>
                </ArrayField>
            </TabbedForm.Tab>
            <TabbedForm.Tab label={'tab.claims'}>
                <SectionTitle
                    text={translate('page.service.claims.header.title')}
                    secondaryText={translate(
                        'page.service.claims.header.subtitle'
                    )}
                />
                <TopToolbar sx={{ width: '100%' }}>
                    <CreateInDialogButton
                        variant="contained"
                        title={translate('ra.page.create', {
                            name: claimLabel,
                        })}
                        maxWidth={'md'}
                        fullWidth
                        resource={
                            'services/' + realmId + '/' + serviceId + '/claims'
                        }
                        mutationOptions={{ onSettled: () => refresh() }}
                        record={{ serviceId }}
                    >
                        <ClaimEditForm mode="create" />
                    </CreateInDialogButton>
                </TopToolbar>
                <ArrayField source="claims">
                    <Datagrid bulkActionButtons={false} sx={{ width: '100%' }}>
                        <NameField
                            text="name"
                            secondaryText="key"
                            source="name"
                            icon={false}
                        />
                        <TextField source="type" />
                        <BooleanField source="multiple" />
                        <RowButtonGroup>
                            <EditInDialogButton
                                title={translate('ra.page.edit', {
                                    name: translate('ra.action.edit'),
                                    recordRepresentation: claimLabel,
                                })}
                                maxWidth={'md'}
                                fullWidth
                                resource={
                                    'services/' +
                                    realmId +
                                    '/' +
                                    serviceId +
                                    '/claims'
                                }
                                queryOptions={{ meta: { root: '' } }}
                                mutationMode="optimistic"
                                mutationOptions={{ onSettled: () => refresh() }}
                            >
                                <ClaimEditForm mode="edit" />
                            </EditInDialogButton>
                            <DeleteWithConfirmButton
                                resource={
                                    'services/' +
                                    realmId +
                                    '/' +
                                    serviceId +
                                    '/claims'
                                }
                                redirect={false}
                                mutationMode="pessimistic"
                                mutationOptions={{ onSettled: () => refresh() }}
                            />
                        </RowButtonGroup>
                    </Datagrid>
                </ArrayField>
                <br />
                <SectionTitle
                    text={translate('page.service.claims.mapping.title')}
                    secondaryText={translate(
                        'page.service.claims.mapping.subtitle'
                    )}
                />
                <ClaimMappingEditor
                    source="claimMapping.user"
                    onTest={handleValidate('user')}
                    defaultValue={claimMappingDefaultValue}
                />

                <ClaimMappingEditor
                    source="claimMapping.client"
                    onTest={handleValidate('client')}
                    defaultValue={claimMappingDefaultValue}
                />
            </TabbedForm.Tab>
        </TabbedForm>
    );
};

const ServiceTabToolbar = () => (
    <Toolbar>
        <SaveButton alwaysEnable />
    </Toolbar>
);

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
