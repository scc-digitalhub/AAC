import { Typography } from '@mui/material';
import {
    EditButton,
    RichTextField,
    Show,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import { PageTitle } from '../components/pageTitle';

export const ServiceShow = () => {
    return (
        <Show actions={<ShowToolBarActions />}>
            <AppTabComponent />
        </Show>
    );
};

const AppTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <PageTitle
                text={record.name}
                secondaryText={record?.id}
            />
            <TabbedShowLayout  syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Overview">
                    <TextField source="id" />
                    <TextField source="name" />
                    <TextField source="description" />
                    <TextField source="namespace" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Settings">
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Scopes">
                </TabbedShowLayout.Tab> 
                <TabbedShowLayout.Tab label="Claims">
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const ShowToolBarActions = () => {
    return (
        <TopToolbar>
            <>
                <EditButton></EditButton>
            </>
        </TopToolbar>
    );
};
