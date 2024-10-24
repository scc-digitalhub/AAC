import { Box, Grid } from '@mui/material';
import {
    useListContext,
    RecordContextProvider,
    SimpleListProps,
    RaRecord,
    useTranslate,
    Identifier,
} from 'react-admin';
import LinearProgress from '@mui/material/LinearProgress';
import { Card, CardContent, CardActions, CardHeader } from '@mui/material';
import { isValidElement, ReactElement, ReactNode } from 'react';

// const style = {
//     // Use flex layout with column direction for components in the card
//     // (CardContent and CardActions)
//     display: 'flex',
//     flexDirection: 'column',

//     // Justify the content so that CardContent will always be at the top of the card,
//     // and CardActions will be at the bottom
//     justifyContent: 'space-between',
// };

export const GridList = <RecordType extends RaRecord = any>(
    props: GridListProps<RecordType>
) => {
    const { cols = 6 } = props;
    const { primaryText, secondaryText, tertiaryText, icon, actions } = props;

    const { data, isLoading } = useListContext<RecordType>(props);
    // const resource = useResourceContext(props);
    const translate = useTranslate();

    if (isLoading === true) {
        return <LinearProgress />;
    }

    if (!data) return null;

    return (
        <Box>
            <Grid container spacing={2}>
                {data.map((record, rowIndex) => (
                    <RecordContextProvider key={record.id} value={record}>
                        <Grid
                            key={record.id}
                            item
                            xs={12}
                            md={cols}
                            zeroMinWidth
                        >
                            <Card sx={{ height: '100%' }}>
                                {!!primaryText &&
                                    (isValidElement(primaryText) ? (
                                        primaryText
                                    ) : (
                                        <CardHeader
                                            title={
                                                typeof primaryText === 'string'
                                                    ? translate(primaryText, {
                                                          ...record,
                                                          _: primaryText,
                                                      })
                                                    : primaryText(
                                                          record,
                                                          record.id
                                                      )
                                            }
                                            titleTypographyProps={{
                                                variant: 'h6',
                                                fontWeight: 600,
                                            }}
                                            subheader={
                                                tertiaryText &&
                                                (typeof tertiaryText ===
                                                'string'
                                                    ? translate(tertiaryText, {
                                                          ...record,
                                                          _: tertiaryText,
                                                      })
                                                    : tertiaryText(
                                                          record,
                                                          record.id
                                                      ))
                                            }
                                            avatar={
                                                icon
                                                    ? isValidElement(icon)
                                                        ? icon
                                                        : icon(
                                                              record,
                                                              record.id
                                                          )
                                                    : false
                                            }
                                        ></CardHeader>
                                    ))}
                                <CardContent>
                                    {secondaryText &&
                                        (typeof secondaryText === 'string'
                                            ? translate(secondaryText, {
                                                  ...record,
                                                  _: secondaryText,
                                              })
                                            : isValidElement(secondaryText)
                                            ? secondaryText
                                            : secondaryText(record, record.id))}
                                </CardContent>
                                {actions && (
                                    <CardActions>
                                        {isValidElement(actions)
                                            ? actions
                                            : actions(record, record.id)}
                                    </CardActions>
                                )}
                            </Card>
                        </Grid>
                    </RecordContextProvider>
                ))}
            </Grid>
        </Box>
    );
};

export type FunctionToElement<RecordType extends RaRecord = any> = (
    record: RecordType,
    id: Identifier
) => ReactNode;

export interface GridListProps<RecordType extends RaRecord = any>
    extends SimpleListProps {
    cols?: number;
    icon?: FunctionToElement<RecordType> | ReactElement;
    tertiaryText?: FunctionToElement<RecordType> | string;
    actions?: FunctionToElement<RecordType> | ReactElement;
}
export default GridList;
