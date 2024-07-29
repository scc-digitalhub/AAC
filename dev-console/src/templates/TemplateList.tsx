import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    useRecordContext,
    Button,
    EditButton,
    ExportButton,
    useRedirect,
    ShowButton,
    useTranslate,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { Box, IconButton, Typography } from '@mui/material';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import { YamlExporter } from '../components/YamlExporter';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useRootSelector } from '@dslab/ra-root-selector';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/page';
import { PageTitle } from '../components/pageTitle';

export const TemplateList = () => {
    useListContext<any>();
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.template.list.title')}
                secondaryText={translate('page.template.list.subtitle')}
            />
            <List
                empty={false}
                exporter={YamlExporter}
                actions={<TemplateListActions />}
                filters={TemplateFilters}
                sort={{ field: 'template', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="template" />
                    <TextField source="language" />
                    <TextField source="authority" />
                    <IdField source="id" />
                    <ShowButton />
                    <EditButton />
                    <DeleteWithDialogButton />
                    <ExportRecordButton />
                </Datagrid>
            </List>
        </Page>
    );
};

const TemplateFilters = [<SearchInput source="q" alwaysOn />];

const TemplateListActions = () => {
    return (
        <TopToolbar>
            <CreateButton variant="contained" />
            <ExportRecordButton />
        </TopToolbar>
    );
};

const Empty = () => {
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No template available, create one
            </Typography>
            <CreateButton variant="contained" />
        </Box>
    );
};

const IdField = (props: any) => {
    let s = props.source;
    const record = useRecordContext();
    if (!record) return null;
    return (
        <span>
            {record[s]}
            <IconButton
                onClick={() => {
                    navigator.clipboard.writeText(record[s]);
                }}
            >
                <ContentCopyIcon />
            </IconButton>
        </span>
    );
};
