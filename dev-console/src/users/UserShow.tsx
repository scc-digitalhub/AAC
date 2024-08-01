import { Box } from '@mui/material';
import {
    ArrayField,
    Datagrid,
    EditButton,
    ReferenceField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { PageTitle } from '../components/pageTitle';
import { Page } from '../components/page';
import { ActiveButton } from './activeButton';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { JsonSchemaField } from '@dslab/ra-jsonschema-input';
import { schemaOAuthClient, uiSchemaOAuthClient } from '../common/schemas';
import { IdField } from '../components/IdField';
import { SectionTitle } from '../components/sectionTitle';

export const UserShow = () => {
    return (
        <Page>
            <Show actions={<ShowToolBarActions />} component={Box}>
                <UserTabComponent />
            </Show>
        </Page>
    );
};

const UserTabComponent = () => {
    const record = useRecordContext();
    const translate = useTranslate();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.username} secondaryText={record?.id} copy={true}/>
            <TabbedShowLayout syncWithLocation={false}>
                <TabbedShowLayout.Tab
                    label={translate('page.user.overview.title')}
                >
                    <TextField source="username" />
                    <TextField source="email" />
                    <TextField source="subjectId" />
                    <TextField source="roles" />
                    <TextField source="permissions" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.account.title')}
                >
                    <SectionTitle
                        text={translate('page.user.account.title')}
                        secondaryText={translate('page.user.account.subTitle')}
                    />
                    <ArrayField source="identities">
                        <Datagrid bulkActionButtons={false}>
                            <TextField source="username" />
                            <TextField source="emailAddress" />
                            <TextField source="provider" />
                        </Datagrid>
                    </ArrayField>
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.audit.title')}
                >
                    <SectionTitle
                        text={translate('page.user.audit.title')}
                        secondaryText={translate('page.user.audit.subTitle')}
                    />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label={translate('page.user.apps.title')}>
                    <SectionTitle
                        text={translate('page.user.apps.title')}
                        secondaryText={translate('page.user.apps.subTitle')}
                    />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.groups.title')}
                >
                    <SectionTitle
                        text={translate('page.user.groups.title')}
                        secondaryText={translate('page.user.groups.subTitle')}
                    />
                    <ArrayField source="groups">
                        <TextField source="name" />
                        <IdField source="id" />
                    </ArrayField>
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.roles.title')}
                >
                    <SectionTitle
                        text={translate('page.user.roles.primaryTitle')}
                        secondaryText={translate('page.user.roles.subTitle')}
                    />
                    <ArrayField source="roles">
                        <Datagrid bulkActionButtons={false}>
                            <TextField source="name" />
                            <IdField source="authority" />
                            <TextField source="provider" />
                        </Datagrid>
                    </ArrayField>
                    <SectionTitle
                        text={translate('page.user.roles.permissionTitle')}
                        secondaryText={translate(
                            'page.user.roles.permissionSubTitle'
                        )}
                    />
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.user.attributes.title')}
                >
                    <SectionTitle
                        text={translate(
                            'page.user.attributes.identityPrimaryTitle'
                        )}
                        secondaryText={translate(
                            'page.user.attributes.identitySubTitle'
                        )}
                    />
                    <SectionTitle
                        text={translate(
                            'page.user.attributes.additionalPrimaryTitle'
                        )}
                        secondaryText={translate(
                            'page.user.attributes.additionalsubTitle'
                        )}
                    />
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label={translate('page.user.tos.title')}>
                    <SectionTitle
                        text={translate('page.user.tos.primaryTitle')}
                        secondaryText={translate('page.user.tos.subTitle')}
                    />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const ShowToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;
    let body = JSON.stringify(record, null, '\t');
    return (
        <TopToolbar>
            <ActiveButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton language="yaml" color="info" />
        </TopToolbar>
    );
};
