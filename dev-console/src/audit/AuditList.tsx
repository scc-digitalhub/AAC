import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    DateField,
    TopToolbar,
    DateInput,
    SelectInput,
    useTranslate,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import ContentFilter from '@mui/icons-material/FilterList';
import { AuditDetails } from './AuditDetails';
import { Typography } from '@mui/material';

import { useForm, FormProvider } from 'react-hook-form';
import { Box, Button } from '@mui/material';
import { Page } from '../components/page';
import { PageTitle } from '../components/pageTitle';
import { YamlExporter } from '../components/YamlExporter';

export const AuditList = () => {
    const translate = useTranslate();
    useListContext<any>();
    return (
        <Page>
            <PageTitle
                text={translate('page.audit.list.title')}
                secondaryText={translate('page.audit.list.subtitle')}
            />
            <List
                actions={<ListActions />}
                empty={false}
                exporter={YamlExporter}
                filters={AuditFilters}
                sort={{ field: 'time', order: 'DESC' }}
                component={Box}
            >
                <Datagrid
                    rowClick="expand"
                    expand={<AuditDetails />}
                    bulkActionButtons={false}
                >
                    <DateField source="time" showTime />
                    <TextField source="type" />
                    <TextField source="principal" />
                </Datagrid>
            </List>
        </Page>
    );
};
const AuditFilters = [<SearchInput source="q" alwaysOn />];

// https://marmelab.com/react-admin/FilteringTutorial.html#building-a-custom-filter
const PostFilterButton = () => {
    const { showFilter } = useListContext();
    return (
        <Button
            size="small"
            color="primary"
            onClick={() => showFilter('main', null)}
            startIcon={<ContentFilter />}
        >
            Filter
        </Button>
    );
};

const PostFilterForm = () => {
    const { displayedFilters, filterValues, setFilters, hideFilter } =
        useListContext();

    const form = useForm({
        defaultValues: filterValues,
    });

    if (!displayedFilters.main) return null;

    const onSubmit = (values: any) => {
        if (Object.keys(values).length > 0) {
            if (values.before) {
                let before = new Date(values.before);
                values.before = before.toISOString();
            }
            if (values.after) {
                let after = new Date(values.after);
                values.after = after.toISOString();
            }
            if (values.type == 'All') {
                values.type = null;
            }
            setFilters(values, null);
        } else {
            hideFilter('main');
        }
    };

    const resetFilter = () => {
        setFilters({}, []);
    };

    return (
        <FormProvider {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)}>
                <Box display="inherit" mb={1}>
                    <Box component="span" mr={2}>
                        <DateInput helperText={false} source="before" />
                    </Box>
                    <Box component="span" mr={2}>
                        <DateInput helperText={false} source="after" />
                    </Box>
                    <Box mr={2}>
                        <SelectInput
                            defaultValue={'All'}
                            source="type"
                            label="Type"
                            choices={[
                                { id: 'All', name: 'All' },
                                {
                                    id: 'USER_AUTHENTICATION_SUCCESS',
                                    name: 'USER_AUTHENTICATION_SUCCESS',
                                },
                                {
                                    id: 'USER_AUTHENTICATION_FAILURE',
                                    name: 'USER_AUTHENTICATION_FAILURE',
                                },
                                {
                                    id: 'CLIENT_AUTHENTICATION_SUCCESS',
                                    name: 'CLIENT_AUTHENTICATION_SUCCESS',
                                },
                                {
                                    id: 'CLIENT_AUTHENTICATION_FAILURE',
                                    name: 'CLIENT_AUTHENTICATION_FAILURE',
                                },
                                { id: 'TOKEN_GRANT', name: 'TOKEN_GRANT' },
                            ]}
                        />
                    </Box>
                    <Box component="span" mr={2}>
                        <Button
                            variant="outlined"
                            color="primary"
                            type="submit"
                        >
                            Filter
                        </Button>
                    </Box>
                    <Box component="span">
                        <Button variant="outlined" onClick={resetFilter}>
                            Close
                        </Button>
                    </Box>
                </Box>
            </form>
        </FormProvider>
    );
};

const ListActions = () => (
    <Box width="100%">
        <TopToolbar>
            <PostFilterButton />
        </TopToolbar>
        <PostFilterForm />
    </Box>
);
